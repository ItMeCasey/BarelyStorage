package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.block.UpgradeableBlockEntity;
import com.itmecasey.barelystorage.item.UpgradeItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One menu for every machine with upgrade cards: Assembler (crafting patterns), Interface Node (processing patterns and the 3x3
 * buffer), Storage Bus (filter and priority), Import and Export Node (filter). Rows of pattern or filter slots beyond the first
 * are locked until Capacity or Filter cards open them.
 */
public class NodeMenu extends StorageMenu {
    public enum Layout { ASSEMBLER, INTERFACE, BUS, IMPORT, EXPORT }

    public static final int UPGRADES = 4;
    public static final int BTN_PRIORITY = 0;

    private final Layout layout;
    private final ContainerData data;
    @Nullable
    private final UpgradeableBlockEntity machine;
    private final Container upgrades;
    private final Container area;
    private final boolean ghost;
    public final int areaX;
    public final int areaY;
    public final int upgradeX;
    public final int upgradeY;
    public final int bufferX;
    public final int bufferY;
    public final int firstPlayerSlot;

    private NodeMenu(MenuType<?> type, Layout layout, int id, Inventory inventory, @Nullable UpgradeableBlockEntity machine, Container upgrades,
                     Container area, @Nullable Container buffer, ContainerData data) {
        super(type, id);
        this.layout = layout;
        this.machine = machine;
        this.upgrades = upgrades;
        this.area = area;
        this.data = data;
        this.ghost = layout == Layout.BUS || layout == Layout.IMPORT || layout == Layout.EXPORT;
        boolean tall = layout == Layout.INTERFACE;
        this.upgradeX = tall ? 80 : 8;
        this.upgradeY = tall ? 30 : 30 + 5 * 18 + 14;
        this.areaX = 8;
        this.areaY = tall ? 98 : 30;
        this.bufferX = 8;
        this.bufferY = 30;
        for (int i = 0; i < UPGRADES; i++) {
            addSlot(new Slot(upgrades, i, upgradeX + i * 18, upgradeY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof UpgradeItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        if (buffer != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addSlot(new Slot(buffer, col + row * 3, bufferX + col * 18, bufferY + row * 18));
                }
            }
        }
        for (int row = 0; row < UpgradeableBlockEntity.MAX_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                final int rowIndex = row;
                addSlot(new Slot(area, index, areaX + col * 18, areaY + row * 18) {
                    @Override
                    public boolean isActive() {
                        return rowIndex <= data.get(ghostOrPattern());
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !ghost && isActive() && container.canPlaceItem(getContainerSlot(), stack);
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return !ghost;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
        this.firstPlayerSlot = slots.size();
        int playerY = tall ? areaY + 5 * 18 + 14 : upgradeY + 18 + 14;
        playerSlots(inventory, 8, playerY);
        this.playerY = playerY;
        addDataSlots(data);
    }

    public final int playerY;

    private int ghostOrPattern() {
        return ghost ? ContainerDataIndex.FILTER : ContainerDataIndex.CAPACITY;
    }

    private static final class ContainerDataIndex {
        static final int CAPACITY = UpgradeableBlockEntity.D_CAPACITY;
        static final int FILTER = UpgradeableBlockEntity.D_FILTER;
    }

    public Layout layout() {
        return layout;
    }

    public boolean ghostArea() {
        return ghost;
    }

    public int priority() {
        return data.get(UpgradeableBlockEntity.D_PRIORITY);
    }

    /** Rows of the pattern or filter area that are open. */
    public int openRows() {
        return 1 + data.get(ghostOrPattern());
    }

    public int areaStart() {
        return UPGRADES + (layout == Layout.INTERFACE ? 9 : 0);
    }

    public static NodeMenu server(Layout layout, int id, Inventory inv, UpgradeableBlockEntity be) {
        return switch (layout) {
            case ASSEMBLER -> new NodeMenu(ModContent.ASSEMBLER_MENU, layout, id, inv, be, be.upgrades, ((AssemblerBlockEntity) be).patterns, null, be.menuData());
            case INTERFACE -> new NodeMenu(ModContent.INTERFACE_MENU, layout, id, inv, be, be.upgrades, ((InterfaceNodeBlockEntity) be).patterns,
                    ((InterfaceNodeBlockEntity) be).buffer, be.menuData());
            case BUS -> new NodeMenu(ModContent.BUS_MENU, layout, id, inv, be, be.upgrades, be.filter, null, be.menuData());
            case IMPORT -> new NodeMenu(ModContent.IMPORT_MENU, layout, id, inv, be, be.upgrades, be.filter, null, be.menuData());
            case EXPORT -> new NodeMenu(ModContent.EXPORT_MENU, layout, id, inv, be, be.upgrades, be.filter, null, be.menuData());
        };
    }

    public static NodeMenu client(Layout layout, int id, Inventory inv) {
        MenuType<?> type = switch (layout) {
            case ASSEMBLER -> ModContent.ASSEMBLER_MENU;
            case INTERFACE -> ModContent.INTERFACE_MENU;
            case BUS -> ModContent.BUS_MENU;
            case IMPORT -> ModContent.IMPORT_MENU;
            case EXPORT -> ModContent.EXPORT_MENU;
        };
        return new NodeMenu(type, layout, id, inv, null, new SimpleContainer(UPGRADES), new SimpleContainer(UpgradeableBlockEntity.MAX_ROWS * 9),
                layout == Layout.INTERFACE ? new SimpleContainer(9) : null, new SimpleContainerData(3));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_PRIORITY && machine instanceof StorageBusBlockEntity bus) {
            if (!player.level().isClientSide()) {
                bus.cyclePriority();
            }
            return true;
        }
        return false;
    }

    /** Ghost filter slots copy the held item (one of it) instead of taking it; clicking with an empty hand clears the slot. */
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (ghost && slotIndex >= areaStart() && slotIndex < areaStart() + UpgradeableBlockEntity.MAX_ROWS * 9) {
            Slot slot = slots.get(slotIndex);
            if (slot.isActive()) {
                ItemStack carried = getCarried();
                area.setItem(slot.getContainerSlot(), carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < firstPlayerSlot) {
            if (!moveItemStackTo(stack, firstPlayerSlot, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof UpgradeItem) {
            if (!moveItemStackTo(stack, 0, UPGRADES, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!ghost && !moveItemStackTo(stack, UPGRADES, firstPlayerSlot, false)) {
            return ItemStack.EMPTY;
        } else if (ghost) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || (!machine.isRemoved() && player.distanceToSqr(machine.getBlockPos().getX() + 0.5, machine.getBlockPos().getY() + 0.5,
                machine.getBlockPos().getZ() + 0.5) <= 64.0);
    }
}
