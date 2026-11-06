package com.itmecasey.barelystorage.block;

import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.item.UpgradeItem;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A machine with four upgrade card slots and a ghost filter (the Assembler, the nodes and the Storage Bus). */
public abstract class UpgradeableBlockEntity extends BlockEntity {
    public static final int MAX_ROWS = 5;
    public static final int D_CAPACITY = 0;
    public static final int D_FILTER = 1;
    public static final int D_PRIORITY = 2;

    public final SimpleContainer upgrades = new SimpleContainer(4) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            UpgradeableBlockEntity.this.setChanged();
        }
    };
    /** Ghost filter slots: they show items and never hold them. */
    public final SimpleContainer filter = new SimpleContainer(MAX_ROWS * 9) {
        @Override
        public void setChanged() {
            super.setChanged();
            UpgradeableBlockEntity.this.setChanged();
        }
    };

    protected UpgradeableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int speed() {
        return 1 + UpgradeItem.count(upgrades, UpgradeItem.Kind.SPEED);
    }

    public int efficiencyCards() {
        return UpgradeItem.count(upgrades, UpgradeItem.Kind.EFFICIENCY);
    }

    public int patternRows() {
        return Math.min(MAX_ROWS, 1 + UpgradeItem.count(upgrades, UpgradeItem.Kind.CAPACITY));
    }

    public int filterRows() {
        return Math.min(MAX_ROWS, 1 + UpgradeItem.count(upgrades, UpgradeItem.Kind.FILTER));
    }

    /** Items named by the active filter rows. */
    public Set<ItemKey> filterKeys(HolderLookup.Provider registries) {
        Set<ItemKey> keys = new LinkedHashSet<>();
        for (int i = 0; i < filterRows() * 9; i++) {
            ItemStack s = filter.getItem(i);
            if (!s.isEmpty()) {
                keys.add(Keys.of(s, registries));
            }
        }
        return keys;
    }

    /** Values the menu shows: capacity cards, filter cards and the Storage Bus priority. */
    public ContainerData menuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_CAPACITY -> patternRows() - 1;
                    case D_FILTER -> filterRows() - 1;
                    default -> priorityOrdinal();
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    protected int priorityOrdinal() {
        return 0;
    }

    protected void saveUpgrades(ValueOutput output) {
        upgrades.storeAsItemList(output.list("upgrades", ItemStack.OPTIONAL_CODEC));
        filter.storeAsItemList(output.list("filter", ItemStack.OPTIONAL_CODEC));
    }

    protected void loadUpgrades(ValueInput input) {
        upgrades.fromItemList(input.listOrEmpty("upgrades", ItemStack.OPTIONAL_CODEC));
        filter.fromItemList(input.listOrEmpty("filter", ItemStack.OPTIONAL_CODEC));
    }

    protected void dropUpgrades(BlockPos pos) {
        if (level != null) {
            Containers.dropContents(level, pos, upgrades);
        }
    }
}
