package com.itmecasey.barelystorage.item;

import com.itmecasey.barelystorage.Settings;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A hardware blade that goes into a Server Rack blade slot: a CPU core, a RAM blade or a Quantum Core. */
public class BladeItem extends Item {
    public enum Kind {
        BASIC_CORE("basic_core"), RAM_BLADE("ram_blade"), QUANTUM_CORE("quantum_core");

        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public int threads() {
            return Settings.getInt("hardware." + key + ".threads");
        }

        public int ram() {
            return Settings.getInt("hardware." + key + ".ram");
        }
    }

    private final Kind kind;

    public BladeItem(Kind kind, Properties properties) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.barelystorage.blade_stats", kind.threads(), kind.ram()));
    }
}
