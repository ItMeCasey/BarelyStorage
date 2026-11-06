package com.itmecasey.barelystorage.item;

import com.itmecasey.barelystorage.Pattern;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

/** A circuit pattern: blank, or encoded with a crafting or processing recipe in its custom data. */
public class PatternItem extends Item {
    public enum Type { BLANK, CRAFTING, PROCESSING }

    private final Type type;

    public PatternItem(Type type, Properties properties) {
        super(properties.stacksTo(type == Type.BLANK ? 64 : 1));
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public static void write(ItemStack stack, Pattern pattern) {
        CompoundTag tag = new CompoundTag();
        tag.putString("p", pattern.encode());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    public static Pattern read(ItemStack stack) {
        if (!(stack.getItem() instanceof PatternItem item) || item.type == Type.BLANK) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        String text = data.copyTag().getStringOr("p", "");
        try {
            return text.isEmpty() ? null : Pattern.decode(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        Pattern p = read(stack);
        if (p == null) {
            out.accept(Component.translatable(type == Type.BLANK ? "tooltip.barelystorage.pattern_blank" : "tooltip.barelystorage.pattern_empty"));
            return;
        }
        for (Pattern.Stack in : p.inputs()) {
            out.accept(Component.translatable("tooltip.barelystorage.pattern_in", in.count(), name(in)));
        }
        for (Pattern.Stack o : p.outputs()) {
            out.accept(Component.translatable("tooltip.barelystorage.pattern_out", o.count(), name(o)));
        }
    }

    private static Component name(Pattern.Stack s) {
        String id = s.key().id();
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        return Component.translatable("item." + ns + "." + path);
    }
}
