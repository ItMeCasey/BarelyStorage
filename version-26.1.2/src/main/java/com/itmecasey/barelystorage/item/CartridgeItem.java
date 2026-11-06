package com.itmecasey.barelystorage.item;

import com.itmecasey.barelystorage.Cartridge;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Tier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/** A storage cartridge. Its contents live in the item's custom data, so a cartridge keeps its items when carried or moved. */
public class CartridgeItem extends Item {
    private final Tier tier;

    public CartridgeItem(Tier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    /** Reads the contents of a cartridge stack. */
    public static Cartridge read(ItemStack stack, Tier tier) {
        Cartridge c = new Cartridge(tier);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return c;
        }
        CompoundTag tag = data.copyTag();
        Map<ItemKey, Long> entries = new LinkedHashMap<>();
        for (net.minecraft.nbt.Tag t : tag.getListOrEmpty("n")) {
            if (t instanceof CompoundTag e) {
                entries.put(ItemKey.decode(e.getStringOr("k", "minecraft:air")), e.getLongOr("c", 0L));
            }
        }
        c.load(entries);
        return c;
    }

    /** Writes the contents of a cartridge into its stack. */
    public static void write(ItemStack stack, Cartridge c) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<ItemKey, Long> e : c.view().entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putString("k", e.getKey().encode());
            one.putLong("c", e.getValue());
            list.add(one);
        }
        tag.put("n", list);
        tag.putLong("used", c.used());
        tag.putInt("types", c.types());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        long used = 0;
        int types = 0;
        if (data != null) {
            CompoundTag tag = data.copyTag();
            used = tag.getLongOr("used", 0L);
            types = tag.getIntOr("types", 0);
        }
        out.accept(Component.translatable("tooltip.barelystorage.cartridge_items", fmt(used), fmt(tier.capacity())));
        out.accept(Component.translatable("tooltip.barelystorage.cartridge_types", types, tier.maxTypes()));
    }

    public static String fmt(long n) {
        if (n >= 1_000_000_000L) {
            return String.format(java.util.Locale.ROOT, "%.1fG", n / 1e9);
        }
        if (n >= 1_000_000L) {
            return String.format(java.util.Locale.ROOT, "%.1fM", n / 1e6);
        }
        if (n >= 10_000L) {
            return String.format(java.util.Locale.ROOT, "%.1fk", n / 1e3);
        }
        return Long.toString(n);
    }
}
