package com.itmecasey.barelystorage;

import net.minecraft.world.item.ItemStack;

/** A view of an inventory outside the network (chest, furnace, modded drawer) seen from one face. */
public interface ExternalInventory {
    int slots();

    /** The stack in a slot; callers must not change it. */
    ItemStack get(int slot);

    /** Puts a stack in anywhere it fits and returns what did not fit. */
    ItemStack insert(ItemStack stack, boolean simulate);

    /** Takes up to {@code amount} items out of one slot. */
    ItemStack extract(int slot, int amount, boolean simulate);
}
