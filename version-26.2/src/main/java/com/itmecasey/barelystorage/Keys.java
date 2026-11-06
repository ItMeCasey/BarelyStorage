package com.itmecasey.barelystorage;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Converts between item stacks and the Minecraft-free {@link ItemKey}. */
public final class Keys {
    private Keys() {
    }

    public static ItemKey of(ItemStack stack, HolderLookup.Provider registries) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return ItemKey.of(id);
        }
        Tag tag = DataComponentPatch.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), patch).result().orElse(null);
        return new ItemKey(id, tag == null ? "" : tag.toString());
    }

    public static ItemStack stack(ItemKey key, int count, HolderLookup.Provider registries) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(key.id()));
        ItemStack stack = new ItemStack(item, count);
        if (!key.data().isEmpty()) {
            try {
                Tag tag = TagParser.parseCompoundFully(key.data());
                DataComponentPatch.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                        .result().ifPresent(stack::applyComponents);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                // A key with broken data gives the plain item rather than crashing the network.
            }
        }
        return stack;
    }

    public static int maxStack(ItemKey key) {
        return Math.max(1, BuiltInRegistries.ITEM.getValue(Identifier.parse(key.id())).getDefaultMaxStackSize());
    }
}
