package com.itmecasey.barelystorage.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/** Base of the mod's menus: adds the player inventory slots. */
public abstract class StorageMenu extends AbstractContainerMenu {
    protected StorageMenu(@Nullable MenuType<?> type, int id) {
        super(type, id);
    }

    /** Three rows of inventory and the hotbar. The first slot added by this call gets the index it is added at. */
    protected final void playerSlots(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, x + col * 18, y + 58));
        }
    }
}
