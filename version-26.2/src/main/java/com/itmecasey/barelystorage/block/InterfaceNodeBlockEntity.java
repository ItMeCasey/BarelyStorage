package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ExternalInventory;
import com.itmecasey.barelystorage.Inventories;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.item.PatternItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * An Interface Node: holds the processing patterns of the machine it faces, and a 1x9 buffer the network fills with that
 * machine's ingredients. The node feeds the buffer into the machine.
 */
public class InterfaceNodeBlockEntity extends UpgradeableBlockEntity {
    public final SimpleContainer buffer = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            InterfaceNodeBlockEntity.this.setChanged();
        }
    };
    /** Processing patterns for this machine; rows beyond {@link #patternRows()} are locked until Capacity cards open them. */
    public final SimpleContainer patterns = new SimpleContainer(MAX_ROWS * 9) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < patternRows() * 9 && stack.getItem() instanceof PatternItem item && item.type() == PatternItem.Type.PROCESSING;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            InterfaceNodeBlockEntity.this.setChanged();
            com.itmecasey.barelystorage.StorageNetwork.invalidate();
        }
    };
    private int age;

    public InterfaceNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.INTERFACE, pos, state);
    }

    @Nullable
    public ExternalInventory inventory() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(NodeBlock.FACING);
        return Inventories.at(level, worldPosition.relative(facing), facing.getOpposite());
    }

    /** True when this node holds the given processing pattern. */
    public boolean holds(Pattern pattern) {
        for (int i = 0; i < patternRows() * 9; i++) {
            if (pattern.equals(PatternItem.read(patterns.getItem(i)))) {
                return true;
            }
        }
        return false;
    }

    public boolean canLoad(Pattern pattern, int runs, HolderLookup.Provider reg) {
        SimpleContainer copy = new SimpleContainer(9);
        for (int i = 0; i < 9; i++) {
            copy.setItem(i, buffer.getItem(i).copy());
        }
        return add(copy, pattern, runs, reg);
    }

    public void load(Pattern pattern, int runs, HolderLookup.Provider reg) {
        add(buffer, pattern, runs, reg);
        setChanged();
    }

    private static boolean add(SimpleContainer target, Pattern pattern, int runs, HolderLookup.Provider reg) {
        for (Pattern.Stack in : pattern.inputs()) {
            long left = (long) in.count() * runs;
            int max = Keys.maxStack(in.key());
            while (left > 0) {
                int chunk = (int) Math.min(max, left);
                ItemStack rest = target.addItem(Keys.stack(in.key(), chunk, reg));
                if (!rest.isEmpty()) {
                    return false;
                }
                left -= chunk;
            }
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InterfaceNodeBlockEntity be) {
        be.tick();
    }

    private void tick() {
        int speed = speed();
        if (++age % Math.max(1, Settings.getInt("nodes.interval_ticks") / speed) != 0 || buffer.isEmpty()) {
            return;
        }
        ExternalInventory inv = inventory();
        if (inv == null) {
            return;
        }
        int budget = Settings.getInt("nodes.items_per_move") * speed;
        for (int i = 0; i < buffer.getContainerSize() && budget > 0; i++) {
            ItemStack s = buffer.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack offer = s.copyWithCount(Math.min(budget, s.getCount()));
            ItemStack rest = inv.insert(offer, false);
            int moved = offer.getCount() - rest.getCount();
            if (moved > 0) {
                s.shrink(moved);
                budget -= moved;
                buffer.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.storeAsItemList(output.list("buffer", ItemStack.OPTIONAL_CODEC));
        patterns.storeAsItemList(output.list("patterns", ItemStack.OPTIONAL_CODEC));
        saveUpgrades(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.fromItemList(input.listOrEmpty("buffer", ItemStack.OPTIONAL_CODEC));
        patterns.fromItemList(input.listOrEmpty("patterns", ItemStack.OPTIONAL_CODEC));
        loadUpgrades(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, buffer);
            Containers.dropContents(level, pos, patterns);
        }
        dropUpgrades(pos);
        com.itmecasey.barelystorage.StorageNetwork.invalidate();
    }
}
