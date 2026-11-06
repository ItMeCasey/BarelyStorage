package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.Cartridge;
import com.itmecasey.barelystorage.CraftPlan;
import com.itmecasey.barelystorage.CraftScheduler;
import com.itmecasey.barelystorage.Hardware;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.PowerModel;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.item.BladeItem;
import com.itmecasey.barelystorage.item.CartridgeItem;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** The Server Rack: drive bays, blade slots, an FE buffer, and the crafting queue when it leads its network. */
public class RackBlockEntity extends BlockEntity {
    public static final int DRIVES = 8;
    public static final int BLADES = 4;
    /** Number of values the rack menu receives. */
    public static final int STATS = 22;

    public final Drives drives = new Drives(this);
    public final SimpleContainer blades = new SimpleContainer(BLADES) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof BladeItem;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            RackBlockEntity.this.setChanged();
        }
    };
    private final CraftScheduler scheduler = new CraftScheduler();
    private final List<String> log = new ArrayList<>();
    private final int[] stats = new int[STATS];
    private int energy;
    private boolean online;
    private int age;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RACK, pos, state);
    }

    public static int driveBays() {
        return Math.max(4, Math.min(DRIVES, Settings.getInt("rack.drive_bays")));
    }

    public static int bladeSlots() {
        return Math.max(1, Math.min(BLADES, Settings.getInt("rack.blade_slots")));
    }

    public boolean online() {
        return online;
    }

    public CraftScheduler scheduler() {
        return scheduler;
    }

    public List<String> log() {
        return log;
    }

    public void addLog(String line) {
        log.add(line);
        while (log.size() > 8) {
            log.remove(0);
        }
        setChanged();
    }

    // ---- Energy ----

    public int energy() {
        return energy;
    }

    public int maxEnergy() {
        return Settings.getInt("power.buffer");
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(maxEnergy(), value));
        setChanged();
    }

    public int receive(int amount, boolean simulate) {
        int take = Math.max(0, Math.min(Math.min(amount, Settings.getInt("power.max_receive")), maxEnergy() - energy));
        if (take > 0 && !simulate) {
            energy += take;
            setChanged();
        }
        return take;
    }

    // ---- Hardware and storage ----

    @Nullable
    public Cartridge cart(int slot) {
        drives.rebuild();
        return slot >= 0 && slot < DRIVES && slot < driveBays() ? drives.carts[slot] : null;
    }

    public void markDirty(int slot) {
        drives.dirty[slot] = true;
        setChanged();
    }

    public int cartridgeCount() {
        int n = 0;
        for (int i = 0; i < driveBays(); i++) {
            if (cart(i) != null) {
                n++;
            }
        }
        return n;
    }

    public long used() {
        long n = 0;
        for (int i = 0; i < driveBays(); i++) {
            Cartridge c = cart(i);
            if (c != null) {
                n += c.used();
            }
        }
        return n;
    }

    public long capacity() {
        long n = 0;
        for (int i = 0; i < driveBays(); i++) {
            Cartridge c = cart(i);
            if (c != null) {
                n += c.capacity();
            }
        }
        return n;
    }

    public Hardware hardware() {
        int basic = 0;
        int ram = 0;
        int quantum = 0;
        for (int i = 0; i < bladeSlots(); i++) {
            if (blades.getItem(i).getItem() instanceof BladeItem b) {
                switch (b.kind()) {
                    case BASIC_CORE -> basic++;
                    case RAM_BLADE -> ram++;
                    case QUANTUM_CORE -> quantum++;
                }
            }
        }
        return Hardware.of(basic, ram, quantum);
    }

    public int upkeep() {
        Hardware hw = hardware();
        return PowerModel.perTick(cartridgeCount(), hw.basicCores(), hw.ramBlades(), hw.quantumCores());
    }

    void onDrivesChanged() {
        setChanged();
        StorageNetwork.invalidate();
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, RackBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        age++;
        int paid = PowerModel.pay(energy, upkeep());
        boolean on = paid >= 0;
        if (on) {
            energy = paid;
        }
        if (on != online) {
            online = on;
            if (state.getValue(RackBlock.ONLINE) != on) {
                level.setBlock(pos, state.setValue(RackBlock.ONLINE, on), 3);
            }
            if (!on) {
                addLog("Power lost: rack offline");
            }
        }
        if (age % 5 == 0 && online) {
            StorageNetwork net = StorageNetwork.find(level, pos);
            if (net.leader() == this) {
                scheduler.setHardware(net.hardware(), net);
                scheduler.tick(net);
            }
        }
        if (age % 10 == 0) {
            refreshStats(level, pos);
        }
    }

    private void refreshStats(Level level, BlockPos pos) {
        Arrays.fill(stats, 0);
        StorageNetwork net = StorageNetwork.find(level, pos);
        long used = net.totalUsed();
        long cap = net.totalCapacity();
        Hardware hw = net.hardware();
        put(0, energy);
        put(2, maxEnergy());
        stats[4] = upkeep();
        put(5, (int) Math.min(Integer.MAX_VALUE, used));
        put(7, (int) Math.min(Integer.MAX_VALUE, cap));
        stats[9] = Math.min(hw.threads(), 32767);
        stats[10] = Math.min(hw.ram(), 32767);
        stats[11] = hw.maxDepth() == Integer.MAX_VALUE ? 99 : hw.maxDepth();
        stats[12] = scheduler.jobs().size();
        stats[16] = online ? 1 : 0;
        stats[17] = driveBays();
        if (!scheduler.jobs().isEmpty()) {
            CraftScheduler.Job head = scheduler.jobs().get(0);
            stats[18] = head.stepsDone();
            stats[19] = head.steps.size();
            stats[14] = head.plan.parallelism;
            stats[15] = Math.min(head.plan.ram, 32767);
            stats[20] = Math.min(head.plan.depth, 99);
            if (head.state == CraftScheduler.State.HALTED) {
                CraftPlan.Error e = head.plan.error;
                stats[13] = e == CraftPlan.Error.NO_CORE ? 1 : e == CraftPlan.Error.THREAD_EXHAUSTION ? 2 : e == CraftPlan.Error.OUT_OF_MEMORY ? 3 : 4;
            }
        }
    }

    private void put(int index, int value) {
        stats[index] = value >>> 16;
        stats[index + 1] = value & 0xFFFF;
    }

    public ContainerData statsData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return stats[index];
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return STATS;
            }
        };
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        drives.flushAll();
        drives.storeAsItemList(output.list("drives", ItemStack.OPTIONAL_CODEC));
        blades.storeAsItemList(output.list("blades", ItemStack.OPTIONAL_CODEC));
        output.putInt("energy", energy);
        output.store("jobs", Codec.STRING.listOf(), scheduler.save());
        output.store("log", Codec.STRING.listOf(), log);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        drives.fromItemList(input.listOrEmpty("drives", ItemStack.OPTIONAL_CODEC));
        drives.rebuild();
        blades.fromItemList(input.listOrEmpty("blades", ItemStack.OPTIONAL_CODEC));
        energy = input.getIntOr("energy", 0);
        input.read("jobs", Codec.STRING.listOf()).ifPresent(scheduler::load);
        log.clear();
        input.read("log", Codec.STRING.listOf()).ifPresent(log::addAll);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            drives.flushAll();
            Containers.dropContents(level, pos, drives);
            Containers.dropContents(level, pos, blades);
        }
        StorageNetwork.invalidate();
    }

    /** The drive bays: a container whose cartridge stacks stay in step with the cartridges the network works on. */
    public static final class Drives extends SimpleContainer {
        private final RackBlockEntity be;
        final Cartridge[] carts = new Cartridge[DRIVES];
        final boolean[] dirty = new boolean[DRIVES];
        private final ItemStack[] seen = new ItemStack[DRIVES];

        Drives(RackBlockEntity be) {
            super(DRIVES);
            this.be = be;
            Arrays.fill(seen, ItemStack.EMPTY);
        }

        void rebuild() {
            for (int i = 0; i < DRIVES; i++) {
                ItemStack s = super.getItem(i);
                if (s.isEmpty()) {
                    carts[i] = null;
                    dirty[i] = false;
                    seen[i] = s;
                } else if (s != seen[i]) {
                    seen[i] = s;
                    carts[i] = s.getItem() instanceof CartridgeItem ci ? CartridgeItem.read(s, ci.tier()) : null;
                    dirty[i] = false;
                }
            }
        }

        void flush(int slot) {
            if (dirty[slot] && carts[slot] != null) {
                CartridgeItem.write(super.getItem(slot), carts[slot]);
                dirty[slot] = false;
            }
        }

        void flushAll() {
            for (int i = 0; i < DRIVES; i++) {
                flush(i);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < driveBays() && stack.getItem() instanceof CartridgeItem;
        }

        @Override
        public ItemStack getItem(int slot) {
            flush(slot);
            return super.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            flush(slot);
            return super.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            flush(slot);
            return super.removeItemNoUpdate(slot);
        }

        @Override
        public void setChanged() {
            rebuild();
            super.setChanged();
            be.onDrivesChanged();
        }
    }
}
