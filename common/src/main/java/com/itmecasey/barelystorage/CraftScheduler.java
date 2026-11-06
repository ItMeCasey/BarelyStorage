package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The crafting queue of one network. Jobs run one at a time, first in first out; inside a job, steps whose inputs are ready run
 * in parallel up to the number of threads the racks offer. A job that does not fit the hardware halts the whole queue with a
 * Thread Exhaustion or Out of Memory message until the hardware changes.
 */
public final class CraftScheduler {
    /** What the scheduler needs from the world. */
    public interface Host {
        /** Takes the inputs of up to {@code runs} runs out of the network and starts them; returns how many runs started (0 when none can yet). */
        int start(Pattern pattern, int runs);

        /** An error or progress line for the rack's log. */
        void log(String line);
    }

    public enum State { RUNNING, QUEUED, HALTED, DONE }

    public static final class StepState {
        public final Pattern pattern;
        public final int total;
        public int pending;
        public int active;
        public long seen;
        public final int[] deps;
        public final int height;

        StepState(Pattern pattern, int total, int pending, int active, long seen, int[] deps, int height) {
            this.pattern = pattern;
            this.total = total;
            this.pending = pending;
            this.active = active;
            this.seen = seen;
            this.deps = deps;
            this.height = height;
        }

        boolean done() {
            return pending == 0 && active == 0;
        }
    }

    public static final class Job {
        public final int id;
        public final ItemKey target;
        public final long amount;
        public final CraftPlan plan;
        public final List<StepState> steps = new ArrayList<>();
        public State state = State.QUEUED;
        public String error = "";

        Job(int id, ItemKey target, long amount, CraftPlan plan) {
            this.id = id;
            this.target = target;
            this.amount = amount;
            this.plan = plan;
        }

        public int stepsDone() {
            int n = 0;
            for (StepState s : steps) {
                if (s.done()) {
                    n++;
                }
            }
            return n;
        }

        boolean finished() {
            return stepsDone() == steps.size();
        }
    }

    private final List<Job> queue = new ArrayList<>();
    private Hardware hardware = Hardware.NONE;
    private int nextId = 1;
    private String lastError = "";

    public Hardware hardware() {
        return hardware;
    }

    public List<Job> jobs() {
        return queue;
    }

    /** The newest error line; empty when the queue is healthy. */
    public String lastError() {
        return lastError;
    }

    public boolean halted() {
        return !queue.isEmpty() && queue.get(0).state == State.HALTED;
    }

    /** Queues a job. The plan is made from the current stock; a plan that cannot be made returns it with its error and queues nothing. */
    public CraftPlan submit(ItemKey target, long amount, Map<ItemKey, Long> stock, List<Pattern> patterns, Host host) {
        CraftPlan plan = CraftPlanner.plan(target, amount, stock, patterns);
        if (!plan.ok()) {
            lastError = plan.message;
            host.log(plan.message);
            return plan;
        }
        if (queue.size() >= Settings.getInt("crafting.job_limit")) {
            plan.error = CraftPlan.Error.MISSING;
            plan.message = "The crafting queue is full";
            return plan;
        }
        Job job = new Job(nextId++, target, amount, plan);
        for (CraftPlan.Step s : plan.steps) {
            int[] deps = s.dependsOn.stream().mapToInt(Integer::intValue).toArray();
            job.steps.add(new StepState(s.pattern, s.runs, s.runs, 0, 0, deps, s.height));
        }
        queue.add(job);
        if (queue.size() == 1) {
            validateHead(host);
        }
        return plan;
    }

    private void validateHead(Host host) {
        if (queue.isEmpty()) {
            return;
        }
        Job head = queue.get(0);
        if (head.plan.fits(hardware)) {
            if (head.state == State.HALTED) {
                host.log("Queue resumed: hardware now fits job " + head.id);
                lastError = "";
            }
            head.state = State.RUNNING;
            head.error = "";
        } else {
            head.state = State.HALTED;
            head.error = head.plan.message;
            lastError = head.plan.message;
            host.log(head.plan.message);
        }
    }

    /** Called when blades are installed or removed. A halted queue tries again. */
    public void setHardware(Hardware hw, Host host) {
        boolean changed = !hw.equals(hardware);
        hardware = hw;
        if (changed && !queue.isEmpty()) {
            validateHead(host);
        }
    }

    public void cancel(int id) {
        boolean wasHead = !queue.isEmpty() && queue.get(0).id == id;
        queue.removeIf(j -> j.id == id);
        if (wasHead) {
            lastError = "";
        }
    }

    /** One scheduler step; call every few ticks. */
    public void tick(Host host) {
        if (queue.isEmpty()) {
            return;
        }
        Job head = queue.get(0);
        if (head.state == State.QUEUED) {
            validateHead(host);
        }
        if (head.state != State.RUNNING) {
            return;
        }
        if (head.finished()) {
            head.state = State.DONE;
            queue.remove(0);
            lastError = "";
            if (!queue.isEmpty()) {
                validateHead(host);
            }
            return;
        }
        int activeSteps = 0;
        for (StepState s : head.steps) {
            if (s.active > 0) {
                activeSteps++;
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < head.steps.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt(i -> head.steps.get(i).height));
        for (int i : order) {
            StepState s = head.steps.get(i);
            if (s.pending <= 0 || activeSteps >= hardware.threads()) {
                continue;
            }
            boolean ready = true;
            for (int d : s.deps) {
                if (!head.steps.get(d).done()) {
                    ready = false;
                    break;
                }
            }
            if (!ready) {
                continue;
            }
            int batch = Math.min(s.pending, Math.max(1, Settings.getInt("crafting.max_batch")));
            int started = Math.max(0, Math.min(batch, host.start(s.pattern, batch)));
            if (started > 0) {
                s.pending -= started;
                if (s.active == 0) {
                    activeSteps++;
                }
                s.active += started;
            }
        }
    }

    /** An Assembler finished {@code runs} runs of a crafting pattern (its outputs are already in the network). */
    public void completed(Pattern pattern, int runs) {
        if (queue.isEmpty()) {
            return;
        }
        for (StepState s : queue.get(0).steps) {
            if (s.pattern.equals(pattern) && s.active > 0) {
                s.active = Math.max(0, s.active - runs);
                return;
            }
        }
    }

    /** Items of {@code key} arrived in the network from an Import Node; they finish runs of processing steps that make that item. */
    public void imported(ItemKey key, long amount) {
        if (queue.isEmpty()) {
            return;
        }
        for (StepState s : queue.get(0).steps) {
            if (s.pattern.kind() == Pattern.Kind.PROCESSING && s.active > 0 && s.pattern.makes(key)) {
                s.seen += amount;
                int per = Math.max(1, s.pattern.outputCount(key));
                int runs = (int) Math.min(s.active, s.seen / per);
                if (runs > 0) {
                    s.active -= runs;
                    s.seen -= (long) runs * per;
                }
                return;
            }
        }
    }

    // ---- Persistence ----

    public List<String> save() {
        List<String> out = new ArrayList<>();
        out.add("Q\t" + nextId + "\t" + lastError.replace('\t', ' '));
        for (Job j : queue) {
            out.add("J\t" + j.id + "\t" + j.target.encode() + "\t" + j.amount + "\t" + j.state.name() + "\t" + j.plan.depth + "\t"
                    + j.plan.parallelism + "\t" + j.plan.ram + "\t" + j.error.replace('\t', ' '));
            for (StepState s : j.steps) {
                StringBuilder deps = new StringBuilder();
                for (int d : s.deps) {
                    deps.append(deps.length() == 0 ? "" : ",").append(d);
                }
                out.add("S\t" + s.pattern.encode() + "\t" + s.total + "\t" + s.pending + "\t" + s.active + "\t" + s.seen + "\t" + deps + "\t" + s.height);
            }
        }
        return out;
    }

    public void load(List<String> lines) {
        queue.clear();
        lastError = "";
        Job job = null;
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "Q" -> {
                    nextId = Integer.parseInt(f[1]);
                    lastError = f.length > 2 ? f[2] : "";
                }
                case "J" -> {
                    ItemKey target = ItemKey.decode(f[2]);
                    long amount = Long.parseLong(f[3]);
                    CraftPlan plan = new CraftPlan(target, amount);
                    plan.depth = Integer.parseInt(f[5]);
                    plan.parallelism = Integer.parseInt(f[6]);
                    plan.ram = Integer.parseInt(f[7]);
                    job = new Job(Integer.parseInt(f[1]), target, amount, plan);
                    job.state = State.valueOf(f[4]);
                    job.error = f.length > 8 ? f[8] : "";
                    queue.add(job);
                }
                case "S" -> {
                    if (job == null) {
                        break;
                    }
                    int[] deps = f[6].isEmpty() ? new int[0]
                            : java.util.Arrays.stream(f[6].split(",")).mapToInt(Integer::parseInt).toArray();
                    Pattern pattern = Pattern.decode(f[1]);
                    StepState s = new StepState(pattern, Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]),
                            Long.parseLong(f[5]), deps, Integer.parseInt(f[7]));
                    job.steps.add(s);
                    CraftPlan.Step ps = new CraftPlan.Step(pattern);
                    ps.runs = s.total;
                    ps.height = s.height;
                    job.plan.steps.add(ps);
                }
                default -> {
                }
            }
        }
        // A loaded RUNNING head waits for the next tick; a HALTED head is revalidated when the hardware is set.
        if (!queue.isEmpty() && queue.get(0).state == State.DONE) {
            queue.remove(0);
        }
    }
}
