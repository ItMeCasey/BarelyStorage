package com.itmecasey.barelystorage.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An upgrade card for an Assembler or a node: Speed, Capacity, Efficiency or Filter. */
public class UpgradeItem extends Item {
    public enum Kind {
        SPEED("speed"), CAPACITY("capacity"), EFFICIENCY("efficiency"), FILTER("filter");

        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private final Kind kind;

    public UpgradeItem(Kind kind, Properties properties) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** How many cards of a kind sit in an upgrade container. */
    public static int count(Container upgrades, Kind kind) {
        int n = 0;
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            if (upgrades.getItem(i).getItem() instanceof UpgradeItem u && u.kind == kind) {
                n++;
            }
        }
        return n;
    }

}
