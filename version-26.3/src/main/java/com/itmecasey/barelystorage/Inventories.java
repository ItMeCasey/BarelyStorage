package com.itmecasey.barelystorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Finds the inventory behind a block face. The loader layer registers the finder that speaks its item capability. */
public final class Inventories {
    @FunctionalInterface
    public interface Finder {
        @Nullable ExternalInventory find(Level level, BlockPos pos, Direction face);
    }

    /** Set by the loader layer; the vanilla container fallback is used when it finds nothing. */
    public static Finder loaderFinder = (level, pos, face) -> null;

    private Inventories() {
    }

    /** The inventory at {@code pos} as seen through its {@code face}. */
    @Nullable
    public static ExternalInventory at(Level level, BlockPos pos, Direction face) {
        ExternalInventory inv = loaderFinder.find(level, pos, face);
        if (inv != null) {
            return inv;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            return new VanillaView(container, face);
        }
        return null;
    }

    /** Fallback for blocks that expose a plain vanilla container and no loader capability. */
    private static final class VanillaView implements ExternalInventory {
        private final Container container;
        private final int[] slots;
        private final WorldlyContainer sided;
        private final Direction face;

        VanillaView(Container container, Direction face) {
            this.container = container;
            this.face = face;
            this.sided = container instanceof WorldlyContainer w ? w : null;
            if (sided != null) {
                this.slots = sided.getSlotsForFace(face);
            } else {
                this.slots = new int[container.getContainerSize()];
                for (int i = 0; i < slots.length; i++) {
                    slots[i] = i;
                }
            }
        }

        @Override
        public int slots() {
            return slots.length;
        }

        @Override
        public ItemStack get(int slot) {
            return container.getItem(slots[slot]);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack rest = stack.copy();
            for (int s : slots) {
                if (rest.isEmpty()) {
                    break;
                }
                if (!container.canPlaceItem(s, rest) || (sided != null && !sided.canPlaceItemThroughFace(s, rest, face))) {
                    continue;
                }
                ItemStack in = container.getItem(s);
                int limit = Math.min(container.getMaxStackSize(), rest.getMaxStackSize());
                if (in.isEmpty()) {
                    int put = Math.min(limit, rest.getCount());
                    if (!simulate) {
                        container.setItem(s, rest.copyWithCount(put));
                    }
                    rest.shrink(put);
                } else if (ItemStack.isSameItemSameComponents(in, rest) && in.getCount() < limit) {
                    int put = Math.min(limit - in.getCount(), rest.getCount());
                    if (!simulate) {
                        in.grow(put);
                    }
                    rest.shrink(put);
                }
            }
            if (!simulate) {
                container.setChanged();
            }
            return rest;
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            int s = slots[slot];
            ItemStack in = container.getItem(s);
            if (in.isEmpty() || (sided != null && !sided.canTakeItemThroughFace(s, in, face))) {
                return ItemStack.EMPTY;
            }
            int take = Math.min(amount, in.getCount());
            ItemStack out = in.copyWithCount(take);
            if (!simulate) {
                container.removeItem(s, take);
                container.setChanged();
            }
            return out;
        }
    }
}
