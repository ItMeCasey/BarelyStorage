package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** What the recipe viewers' plus buttons do in a Crafting or Pattern Terminal: set the grid slots to the recipe's ingredients. */
public final class ViewerFill {
    private ViewerFill() {
    }

    /** True for a terminal whose grid a recipe can be sent to. */
    public static boolean accepts(TerminalMenu menu) {
        return menu.kind() != TerminalMenu.Kind.STORAGE;
    }

    /**
     * Picks which of the acceptable items to put in a slot: one you carry, else one the terminal's list shows, else the first.
     */
    public static ItemStack pick(List<ItemStack> candidates, TerminalMenu menu, Player player) {
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack c : candidates) {
            if (c.isEmpty()) {
                continue;
            }
            if (first.isEmpty()) {
                first = c;
            }
            if (player.getInventory().contains(s -> ItemStack.isSameItem(s, c))) {
                return c;
            }
        }
        for (ItemStack c : candidates) {
            for (int i = 0; i < menu.viewSlots(); i++) {
                Slot slot = menu.slots.get(i);
                if (!c.isEmpty() && slot.hasItem() && ItemStack.isSameItem(slot.getItem(), c)) {
                    return c;
                }
            }
        }
        return first;
    }

    /** Sends the nine grid choices (EMPTY clears a slot) to the server through menu buttons. */
    public static void send(TerminalMenu menu, ItemStack[] grid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return;
        }
        for (int i = 0; i < TerminalMenu.GRID; i++) {
            ItemStack s = i < grid.length && grid[i] != null ? grid[i] : ItemStack.EMPTY;
            int raw = s.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(s.getItem());
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TerminalMenu.BTN_SET + i * 10000 + raw);
        }
    }
}
