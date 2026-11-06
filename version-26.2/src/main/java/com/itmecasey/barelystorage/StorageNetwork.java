package com.itmecasey.barelystorage;

import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.ImportNodeBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.item.PatternItem;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * One connected storage network, found by walking the node blocks from any member. Holds the merged storage, the racks that
 * power it and the machines that execute crafting. Cheap to rebuild: the walk is cached until a node block changes.
 */
public final class StorageNetwork implements CraftScheduler.Host {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LIMIT = 4096;
    private static int topology;
    private record Snapshot(int topology, Set<Long> members) {
    }

    private static final Map<Level, Map<Long, Snapshot>> CACHE = new WeakHashMap<>();

    /** Called whenever a node block is placed, removed or changes shape, so cached walks are redone. */
    public static void invalidate() {
        topology++;
    }

    public final Level level;
    public final List<RackBlockEntity> racks = new ArrayList<>();
    public final List<StorageBusBlockEntity> buses = new ArrayList<>();
    public final List<InterfaceNodeBlockEntity> interfaces = new ArrayList<>();
    public final List<ImportNodeBlockEntity> imports = new ArrayList<>();
    public final List<AssemblerBlockEntity> assemblers = new ArrayList<>();
    private StoragePool pool;
    private List<Pattern> patterns;

    private StorageNetwork(Level level) {
        this.level = level;
    }

    /** The network that the block at {@code start} belongs to (empty when it is not a node). */
    public static StorageNetwork find(Level level, BlockPos start) {
        Map<Long, Snapshot> perLevel = CACHE.computeIfAbsent(level, l -> new HashMap<>());
        Snapshot snap = perLevel.get(start.asLong());
        if (snap == null || snap.topology != topology) {
            Set<Long> members = NetGraph.component(start.asLong(), at -> neighbors(level, BlockPos.of(at)), LIMIT);
            snap = new Snapshot(topology, members);
            if (perLevel.size() > 512) {
                perLevel.clear();
            }
            perLevel.put(start.asLong(), snap);
        }
        StorageNetwork net = new StorageNetwork(level);
        for (long m : snap.members) {
            BlockEntity be = level.getBlockEntity(BlockPos.of(m));
            if (be instanceof RackBlockEntity r) {
                net.racks.add(r);
            } else if (be instanceof StorageBusBlockEntity b) {
                net.buses.add(b);
            } else if (be instanceof InterfaceNodeBlockEntity i) {
                net.interfaces.add(i);
            } else if (be instanceof ImportNodeBlockEntity i) {
                net.imports.add(i);
            } else if (be instanceof AssemblerBlockEntity a) {
                net.assemblers.add(a);
            }
        }
        return net;
    }

    private static long[] neighbors(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetNode node)) {
            return new long[0];
        }
        long[] out = new long[6];
        int n = 0;
        for (Direction d : Direction.values()) {
            if (!node.connects(state, d)) {
                continue;
            }
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) {
                continue;
            }
            BlockState ns = level.getBlockState(np);
            if (ns.getBlock() instanceof NetNode other && other.connects(ns, d.getOpposite())) {
                out[n++] = np.asLong();
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    public HolderLookup.Provider registries() {
        return level.registryAccess();
    }

    public boolean online() {
        for (RackBlockEntity r : racks) {
            if (r.online()) {
                return true;
            }
        }
        return false;
    }

    /** The online rack that runs the crafting queue: the one with the lowest position. */
    @Nullable
    public RackBlockEntity leader() {
        RackBlockEntity best = null;
        for (RackBlockEntity r : racks) {
            if (r.online() && (best == null || r.getBlockPos().asLong() < best.getBlockPos().asLong())) {
                best = r;
            }
        }
        return best;
    }

    public Hardware hardware() {
        Hardware hw = Hardware.NONE;
        for (RackBlockEntity r : racks) {
            if (r.online()) {
                hw = hw.plus(r.hardware());
            }
        }
        return hw;
    }

    public StoragePool pool() {
        if (pool == null) {
            pool = new StoragePool();
            if (online()) {
                for (RackBlockEntity r : racks) {
                    if (r.online()) {
                        for (int i = 0; i < r.driveBays(); i++) {
                            if (r.cart(i) != null) {
                                pool.add(new RackProvider(r, i));
                            }
                        }
                    }
                }
                for (StorageBusBlockEntity b : buses) {
                    pool.add(new BusProvider(b, registries()));
                }
            }
        }
        return pool;
    }

    public List<Pattern> patterns() {
        if (patterns == null) {
            patterns = new ArrayList<>();
            if (online()) {
                for (AssemblerBlockEntity a : assemblers) {
                    for (int i = 0; i < a.patterns.getContainerSize(); i++) {
                        Pattern p = PatternItem.read(a.patterns.getItem(i));
                        if (p != null) {
                            patterns.add(p);
                        }
                    }
                }
            }
        }
        return patterns;
    }

    public long totalUsed() {
        long n = 0;
        for (RackBlockEntity r : racks) {
            if (r.online()) {
                n += r.used();
            }
        }
        return n;
    }

    public long totalCapacity() {
        long n = 0;
        for (RackBlockEntity r : racks) {
            if (r.online()) {
                n += r.capacity();
            }
        }
        return n;
    }

    // ---- Item operations used by terminals and nodes ----

    /** Puts a stack into the network; returns what did not fit. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty() || !online()) {
            return stack;
        }
        ItemKey key = Keys.of(stack, registries());
        long in = pool().insert(key, stack.getCount(), false);
        return in >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount((int) (stack.getCount() - in));
    }

    /** Takes up to {@code amount} (at most one stack) of an item out of the network. */
    public ItemStack extract(ItemKey key, int amount) {
        if (!online()) {
            return ItemStack.EMPTY;
        }
        int want = Math.min(amount, Keys.maxStack(key));
        long got = pool().extract(key, want, false);
        return got <= 0 ? ItemStack.EMPTY : Keys.stack(key, (int) got, registries());
    }

    /** Items from an Import Node: stored, and counted toward crafting steps that wait for them. */
    public ItemStack importStack(ItemStack stack) {
        ItemKey key = Keys.of(stack, registries());
        ItemStack rest = insert(stack);
        int taken = stack.getCount() - rest.getCount();
        RackBlockEntity lead = leader();
        if (taken > 0 && lead != null) {
            lead.scheduler().imported(key, taken);
        }
        return rest;
    }

    /** Queues a crafting job on the leader rack and returns the plan (check {@code ok()} and {@code message}). */
    public CraftPlan requestCraft(ItemKey key, long amount) {
        RackBlockEntity lead = leader();
        if (lead == null) {
            CraftPlan p = CraftPlanner.plan(key, amount, java.util.Map.of(), patterns());
            p.error = CraftPlan.Error.NO_CORE;
            p.message = "The network is offline";
            return p;
        }
        lead.scheduler().setHardware(hardware(), this);
        CraftPlan plan = lead.scheduler().submit(key, amount, pool().view(), patterns(), this);
        lead.setChanged();
        return plan;
    }

    // ---- CraftScheduler.Host ----

    @Override
    public int start(Pattern pattern, int runs) {
        long fit = runs;
        for (Pattern.Stack in : pattern.inputs()) {
            fit = Math.min(fit, pool().count(in.key()) / Math.max(1, in.count()));
        }
        if (fit <= 0) {
            return 0;
        }
        if (pattern.kind() == Pattern.Kind.CRAFTING) {
            for (AssemblerBlockEntity a : assemblers) {
                if (!a.busy()) {
                    int r = (int) fit;
                    takeInputs(pattern, r);
                    a.begin(pattern, r);
                    return r;
                }
            }
            return 0;
        }
        for (InterfaceNodeBlockEntity node : interfaces) {
            if (!node.accepts(pattern, registries())) {
                continue;
            }
            for (int r = (int) fit; r >= 1; r--) {
                if (node.canLoad(pattern, r, registries())) {
                    takeInputs(pattern, r);
                    node.load(pattern, r, registries());
                    return r;
                }
            }
        }
        return 0;
    }

    private void takeInputs(Pattern pattern, int runs) {
        for (Pattern.Stack in : pattern.inputs()) {
            pool().extract(in.key(), (long) in.count() * runs, false);
        }
    }

    @Override
    public void log(String line) {
        LOGGER.warn("[BarelyStorage] {}", line);
        RackBlockEntity lead = leader();
        if (lead != null) {
            lead.addLog(line);
        }
    }

    // ---- Providers ----

    private static final class RackProvider implements StorageProvider {
        private final RackBlockEntity rack;
        private final int slot;

        RackProvider(RackBlockEntity rack, int slot) {
            this.rack = rack;
            this.slot = slot;
        }

        @Override
        public Priority priority() {
            return Priority.NORMAL;
        }

        @Override
        public void collect(Map<ItemKey, Long> out) {
            Cartridge c = rack.cart(slot);
            if (c != null) {
                c.view().forEach((k, v) -> out.merge(k, v, Long::sum));
            }
        }

        @Override
        public long count(ItemKey key) {
            Cartridge c = rack.cart(slot);
            return c == null ? 0 : c.count(key);
        }

        @Override
        public long insert(ItemKey key, long amount, boolean simulate) {
            Cartridge c = rack.cart(slot);
            long n = c == null ? 0 : c.insert(key, amount, simulate);
            if (n > 0 && !simulate) {
                rack.markDirty(slot);
            }
            return n;
        }

        @Override
        public long extract(ItemKey key, long amount, boolean simulate) {
            Cartridge c = rack.cart(slot);
            long n = c == null ? 0 : c.extract(key, amount, simulate);
            if (n > 0 && !simulate) {
                rack.markDirty(slot);
            }
            return n;
        }
    }

    /** An inventory behind a Storage Bus, mapped into the network in place. */
    private static final class BusProvider implements StorageProvider {
        private final StorageBusBlockEntity bus;
        private final HolderLookup.Provider reg;

        BusProvider(StorageBusBlockEntity bus, HolderLookup.Provider reg) {
            this.bus = bus;
            this.reg = reg;
        }

        @Override
        public Priority priority() {
            return bus.priority();
        }

        @Override
        public void collect(Map<ItemKey, Long> out) {
            ExternalInventory inv = bus.inventory();
            if (inv == null) {
                return;
            }
            for (int i = 0; i < inv.slots(); i++) {
                ItemStack s = inv.get(i);
                if (!s.isEmpty() && !inv.extract(i, 1, true).isEmpty()) {
                    out.merge(Keys.of(s, reg), (long) s.getCount(), Long::sum);
                }
            }
        }

        @Override
        public long count(ItemKey key) {
            ExternalInventory inv = bus.inventory();
            long n = 0;
            if (inv != null) {
                for (int i = 0; i < inv.slots(); i++) {
                    ItemStack s = inv.get(i);
                    if (!s.isEmpty() && Keys.of(s, reg).equals(key) && !inv.extract(i, 1, true).isEmpty()) {
                        n += s.getCount();
                    }
                }
            }
            return n;
        }

        @Override
        public long insert(ItemKey key, long amount, boolean simulate) {
            ExternalInventory inv = bus.inventory();
            if (inv == null) {
                return 0;
            }
            long done = 0;
            int max = Keys.maxStack(key);
            while (done < amount) {
                int chunk = (int) Math.min(max, amount - done);
                ItemStack rest = inv.insert(Keys.stack(key, chunk, reg), simulate);
                int took = chunk - rest.getCount();
                done += took;
                if (took < chunk || simulate) {
                    // A simulated insert does not change the inventory, so asking again would count the same room twice.
                    break;
                }
            }
            return done;
        }

        @Override
        public long extract(ItemKey key, long amount, boolean simulate) {
            ExternalInventory inv = bus.inventory();
            if (inv == null) {
                return 0;
            }
            long got = 0;
            for (int i = 0; i < inv.slots() && got < amount; i++) {
                ItemStack s = inv.get(i);
                if (!s.isEmpty() && Keys.of(s, reg).equals(key)) {
                    got += inv.extract(i, (int) Math.min(s.getCount(), amount - got), simulate).getCount();
                }
            }
            return got;
        }
    }
}
