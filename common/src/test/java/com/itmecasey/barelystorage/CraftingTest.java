package com.itmecasey.barelystorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CraftingTest {
    static final ItemKey LOG = ItemKey.of("minecraft:oak_log");
    static final ItemKey PLANKS = ItemKey.of("minecraft:oak_planks");
    static final ItemKey STICK = ItemKey.of("minecraft:stick");
    static final ItemKey PICK = ItemKey.of("minecraft:wooden_pickaxe");
    static final ItemKey ORE = ItemKey.of("minecraft:raw_iron");
    static final ItemKey INGOT = ItemKey.of("minecraft:iron_ingot");

    static Pattern craft(ItemKey out, int outCount, Object... in) {
        List<Pattern.Stack> inputs = new ArrayList<>();
        for (int i = 0; i < in.length; i += 2) {
            inputs.add(new Pattern.Stack((ItemKey) in[i], (Integer) in[i + 1]));
        }
        return new Pattern(Pattern.Kind.CRAFTING, inputs, List.of(new Pattern.Stack(out, outCount)));
    }

    final Pattern planksP = craft(PLANKS, 4, LOG, 1);
    final Pattern stickP = craft(STICK, 4, PLANKS, 2);
    final Pattern pickP = craft(PICK, 1, PLANKS, 3, STICK, 2);
    final List<Pattern> all = List.of(planksP, stickP, pickP);

    static final class FakeHost implements CraftScheduler.Host {
        final List<String> log = new ArrayList<>();
        final List<Pattern> started = new ArrayList<>();
        boolean accept = true;

        public int start(Pattern pattern, int runs) {
            if (accept) {
                started.add(pattern);
            }
            return accept ? runs : 0;
        }

        public void log(String line) {
            log.add(line);
        }
    }

    @BeforeEach
    void reset() {
        Settings.reset();
    }

    Map<ItemKey, Long> stock(Object... kv) {
        Map<ItemKey, Long> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((ItemKey) kv[i], ((Number) kv[i + 1]).longValue());
        }
        return m;
    }

    @Test
    void usesStockBeforeCrafting() {
        CraftPlan plan = CraftPlanner.plan(PICK, 1, stock(PLANKS, 10, STICK, 10), all);
        assertTrue(plan.ok());
        assertEquals(1, plan.steps.size());
        assertEquals(1, plan.depth);
        assertEquals(1, plan.parallelism);
    }

    @Test
    void recursesThroughPatternsAndRoundsRunsUp() {
        CraftPlan plan = CraftPlanner.plan(PICK, 1, stock(LOG, 5), all);
        assertTrue(plan.ok(), plan.message);
        // pick needs 3 planks + 2 sticks; sticks need 2 planks (1 run); planks needed = 5 -> 2 runs of planks.
        assertEquals(3, plan.steps.size());
        assertEquals(3, plan.depth);
        int planksRuns = plan.steps.stream().filter(s -> s.pattern.equals(planksP)).mapToInt(s -> s.runs).sum();
        assertEquals(2, planksRuns);
        assertEquals(2L, plan.fromStock.get(LOG));
    }

    @Test
    void reportsMissingIngredients() {
        CraftPlan plan = CraftPlanner.plan(PICK, 1, stock(), all);
        assertEquals(CraftPlan.Error.MISSING, plan.error);
        assertTrue(plan.missing.containsKey(LOG));
        assertEquals(CraftPlan.Error.NO_PATTERN, CraftPlanner.plan(INGOT, 1, stock(), all).error);
    }

    @Test
    void detectsPatternLoops() {
        Pattern a = craft(PLANKS, 1, LOG, 1);
        Pattern b = craft(LOG, 1, PLANKS, 1);
        CraftPlan plan = CraftPlanner.plan(PLANKS, 1, stock(), List.of(a, b));
        assertEquals(CraftPlan.Error.CYCLE, plan.error);
    }

    @Test
    void basicCoreRunsOnlyLinearCrafts() {
        Hardware basic = Hardware.of(1, 0, 0);
        CraftPlan linear = CraftPlanner.plan(PICK, 1, stock(PLANKS, 10, STICK, 10), all);
        assertTrue(linear.fits(basic));
        CraftPlan deep = CraftPlanner.plan(PICK, 1, stock(LOG, 5), all);
        assertFalse(deep.fits(basic));
        assertEquals(CraftPlan.Error.THREAD_EXHAUSTION, deep.error);
        assertTrue(deep.message.startsWith("Thread Exhaustion"));
    }

    @Test
    void ramBladeAllowsDepthThreeAndQuantumUnlimited() {
        CraftPlan deep = CraftPlanner.plan(PICK, 1, stock(LOG, 5), all);
        assertTrue(deep.fits(Hardware.of(1, 1, 0)), deep.message);
        // A 4 deep chain needs a Quantum Core.
        Pattern axe = craft(ItemKey.of("minecraft:axe"), 1, PICK, 1);
        List<Pattern> more = new ArrayList<>(all);
        more.add(axe);
        CraftPlan four = CraftPlanner.plan(ItemKey.of("minecraft:axe"), 1, stock(LOG, 5), more);
        assertEquals(4, four.depth);
        assertFalse(four.fits(Hardware.of(1, 3, 0)));
        assertTrue(four.fits(Hardware.of(0, 0, 1)));
    }

    @Test
    void noCoreMeansNoCrafting() {
        CraftPlan plan = CraftPlanner.plan(PICK, 1, stock(PLANKS, 10, STICK, 10), all);
        assertFalse(plan.fits(Hardware.of(0, 3, 0)));
        assertEquals(CraftPlan.Error.NO_CORE, plan.error);
    }

    @Test
    void outOfMemoryWhenRamTooSmall() {
        Settings.set("hardware.basic_core.ram", 2);
        CraftPlan plan = CraftPlanner.plan(PICK, 1, stock(PLANKS, 10, STICK, 10), all);
        assertFalse(plan.fits(Hardware.of(1, 0, 0)));
        assertEquals(CraftPlan.Error.OUT_OF_MEMORY, plan.error);
        assertTrue(plan.message.startsWith("Out of Memory"));
    }

    @Test
    void parallelismCountsStepsOnTheSameLevel() {
        ItemKey a = ItemKey.of("a");
        ItemKey b = ItemKey.of("b");
        ItemKey top = ItemKey.of("top");
        List<Pattern> ps = List.of(craft(a, 1, LOG, 1), craft(b, 1, ORE, 1), craft(top, 1, a, 1, b, 1));
        CraftPlan plan = CraftPlanner.plan(top, 1, stock(LOG, 1, ORE, 1), ps);
        assertTrue(plan.ok());
        assertEquals(2, plan.depth);
        assertEquals(2, plan.parallelism);
        assertFalse(plan.fits(Hardware.of(1, 0, 0)), "one thread cannot run both");
        Settings.set("hardware.ram_blade.threads", 4);
        assertTrue(plan.fits(Hardware.of(1, 1, 0)), plan.message);
    }

    @Test
    void schedulerHaltsThenResumesWhenHardwareArrives() {
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(1, 0, 0), host);
        s.submit(PICK, 1, stock(LOG, 5), all, host);
        assertTrue(s.halted());
        assertTrue(s.lastError().startsWith("Thread Exhaustion"));
        assertTrue(host.log.get(0).startsWith("Thread Exhaustion"));
        s.tick(host);
        assertTrue(host.started.isEmpty(), "a halted queue starts nothing");
        s.setHardware(Hardware.of(1, 1, 0), host);
        assertFalse(s.halted());
        assertEquals("", s.lastError());
        s.tick(host);
        assertEquals(1, host.started.size());
    }

    @Test
    void schedulerRunsStepsInDependencyOrderAndFinishes() {
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(0, 0, 1), host);
        s.submit(PICK, 1, stock(LOG, 5), all, host);
        s.tick(host);
        assertEquals(List.of(planksP), host.started, "only the bottom step is ready");
        s.tick(host);
        assertEquals(1, host.started.size(), "the planks step is still active");
        s.completed(planksP, 2);
        s.tick(host);
        assertEquals(stickP, host.started.get(1));
        s.completed(stickP, 1);
        s.tick(host);
        assertEquals(pickP, host.started.get(2));
        s.completed(pickP, 1);
        s.tick(host);
        assertTrue(s.jobs().isEmpty());
    }

    @Test
    void threadLimitCapsParallelSteps() {
        ItemKey a = ItemKey.of("a");
        ItemKey b = ItemKey.of("b");
        ItemKey top = ItemKey.of("top");
        List<Pattern> ps = List.of(craft(a, 1, LOG, 1), craft(b, 1, ORE, 1), craft(top, 1, a, 1, b, 1));
        Settings.set("hardware.ram_blade.threads", 1);
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(1, 1, 0), host);
        s.submit(top, 1, stock(LOG, 1, ORE, 1), ps, host);
        s.tick(host);
        assertEquals(2, host.started.size(), "two threads run both leaf steps in one tick");
    }

    @Test
    void importedItemsFinishProcessingRuns() {
        Pattern smelt = new Pattern(Pattern.Kind.PROCESSING, List.of(new Pattern.Stack(ORE, 1)), List.of(new Pattern.Stack(INGOT, 1)));
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(1, 0, 0), host);
        s.submit(INGOT, 3, stock(ORE, 3), List.of(smelt), host);
        s.tick(host);
        assertEquals(3, s.jobs().get(0).steps.get(0).active);
        s.imported(INGOT, 2);
        assertEquals(1, s.jobs().get(0).steps.get(0).active);
        s.imported(INGOT, 1);
        s.tick(host);
        assertTrue(s.jobs().isEmpty());
    }

    @Test
    void failedStartRetriesLater() {
        FakeHost host = new FakeHost();
        host.accept = false;
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(1, 0, 0), host);
        s.submit(PICK, 1, stock(PLANKS, 10, STICK, 10), all, host);
        s.tick(host);
        assertEquals(1, s.jobs().get(0).steps.get(0).pending);
        host.accept = true;
        s.tick(host);
        assertEquals(1, host.started.size());
    }

    @Test
    void queueStateSurvivesSaveAndLoad() {
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(0, 0, 1), host);
        s.submit(PICK, 1, stock(LOG, 5), all, host);
        s.tick(host);
        List<String> saved = s.save();
        CraftScheduler t = new CraftScheduler();
        t.load(saved);
        t.setHardware(Hardware.of(0, 0, 1), host);
        assertEquals(1, t.jobs().size());
        assertEquals(s.jobs().get(0).steps.size(), t.jobs().get(0).steps.size());
        assertEquals(s.jobs().get(0).steps.get(0).active, t.jobs().get(0).steps.get(0).active);
        assertEquals(saved, t.save());
        t.completed(planksP, 2);
        t.tick(host);
        assertEquals(stickP, host.started.get(1));
    }

    @Test
    void haltedQueueStateSurvivesSaveAndLoad() {
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(1, 0, 0), host);
        s.submit(PICK, 1, stock(LOG, 5), all, host);
        CraftScheduler t = new CraftScheduler();
        t.load(s.save());
        assertTrue(t.halted());
        t.setHardware(Hardware.of(1, 0, 0), host);
        t.setHardware(Hardware.of(0, 0, 1), host);
        assertFalse(t.halted());
    }

    @Test
    void patternRoundTrips() {
        Pattern p = new Pattern(Pattern.Kind.PROCESSING,
                List.of(new Pattern.Stack(ORE, 2), new Pattern.Stack(new ItemKey("x:y", "{a:1}"), 1)),
                List.of(new Pattern.Stack(INGOT, 3)));
        assertEquals(p, Pattern.decode(p.encode()));
    }

    @Test
    void fullQueueRejectsNewJobs() {
        Settings.set("crafting.job_limit", 1);
        FakeHost host = new FakeHost();
        CraftScheduler s = new CraftScheduler();
        s.setHardware(Hardware.of(0, 0, 1), host);
        assertTrue(s.submit(PICK, 1, stock(PLANKS, 9, STICK, 9), all, host).ok());
        assertFalse(s.submit(PICK, 1, stock(PLANKS, 9, STICK, 9), all, host).ok());
    }

    @Test
    void tagSubstitutesUseWhatIsInStock() {
        ItemKey birch = ItemKey.of("minecraft:birch_planks");
        Pattern sticksFromAnyPlanks = new Pattern(Pattern.Kind.CRAFTING,
                List.of(new Pattern.Stack(PLANKS, 2, List.of(birch))), List.of(new Pattern.Stack(STICK, 4)));
        CraftPlan plan = CraftPlanner.plan(STICK, 4, stock(birch, 10), List.of(sticksFromAnyPlanks));
        assertTrue(plan.ok(), plan.message);
        assertEquals(1, plan.steps.size());
        assertEquals(2L, plan.fromStock.get(birch));
        CraftPlan strict = CraftPlanner.plan(STICK, 4, stock(birch, 10),
                List.of(new Pattern(Pattern.Kind.CRAFTING, List.of(new Pattern.Stack(PLANKS, 2)), List.of(new Pattern.Stack(STICK, 4)))));
        assertEquals(CraftPlan.Error.MISSING, strict.error);
    }

    @Test
    void substitutesSurviveEncoding() {
        Pattern p = new Pattern(Pattern.Kind.CRAFTING,
                List.of(new Pattern.Stack(PLANKS, 3, List.of(ItemKey.of("minecraft:birch_planks"), ItemKey.of("minecraft:oak_planks"))), new Pattern.Stack(STICK, 2)),
                List.of(new Pattern.Stack(PICK, 1)));
        assertEquals(p, Pattern.decode(p.encode()));
    }
}
