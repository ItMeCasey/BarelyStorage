package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.ModContent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A 3x3 slot grid over a container: the Interface Node buffer and the Assembler's pattern slots. */
public class GridMenu extends StorageMenu {
    private final Container grid;

    private GridMenu(MenuType<?> type, int id, Inventory inventory, Container grid) {
        super(type, id);
        this.grid = grid;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(grid, col + row * 3, 62 + col * 18, 20 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        playerSlots(inventory, 8, 84);
    }

    public static GridMenu interfaceNode(int id, Inventory inv, Container buffer) {
        return new GridMenu(ModContent.INTERFACE_MENU, id, inv, buffer);
    }

    public static GridMenu assembler(int id, Inventory inv, Container patterns) {
        return new GridMenu(ModContent.ASSEMBLER_MENU, id, inv, patterns);
    }

    public static GridMenu clientInterface(int id, Inventory inv) {
        return interfaceNode(id, inv, new SimpleContainer(9));
    }

    public static GridMenu clientAssembler(int id, Inventory inv) {
        return assembler(id, inv, new SimpleContainer(9));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 9, false)) {
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
        return grid.stillValid(player);
    }
}
