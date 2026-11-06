package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.menu.RackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Server Rack screen: drives, blades, and a readout of power, storage, hardware load and errors. */
public class RackScreen extends AbstractContainerScreen<RackMenu> {
    public RackScreen(RackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 212);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 119;
        this.inventoryLabelX = 8;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.panel(g, leftPos + 5, topPos + 70, 166, 46);
        Ui.slots(g, menu.slots, leftPos, topPos);
        int bays = menu.stat(17);
        for (int i = 0; i < 8; i++) {
            if (bays > 0 && i >= bays) {
                g.fill(leftPos + RackMenu.DRIVE_X + i * 18, topPos + RackMenu.DRIVE_Y,
                        leftPos + RackMenu.DRIVE_X + i * 18 + 16, topPos + RackMenu.DRIVE_Y + 16, 0xCC000000);
            }
        }
        for (int i = 4; i < 8; i++) {
            // Blade slots beyond the first four do not exist; nothing to draw.
        }
        boolean online = menu.stat(16) != 0;
        g.fill(leftPos + 158, topPos + 8, leftPos + 168, topPos + 12, online ? Ui.GREEN : Ui.RED);
        // Power
        int energy = menu.stat32(0);
        int cap = Math.max(1, menu.stat32(2));
        g.text(font, Component.translatable("gui.barelystorage.power", energy, cap, menu.stat(4)), leftPos + 9, topPos + 74, Ui.TEXT, false);
        Ui.bar(g, leftPos + 9, topPos + 84, 158, 7, (double) energy / cap, Ui.GOLD);
        // Storage
        long used = menu.stat32(5) & 0xFFFFFFFFL;
        long total = Math.max(1, menu.stat32(7) & 0xFFFFFFFFL);
        g.text(font, Component.translatable("gui.barelystorage.storage", CartridgeItem.fmt(used), CartridgeItem.fmt(total)), leftPos + 9, topPos + 93, Ui.TEXT, false);
        Ui.bar(g, leftPos + 9, topPos + 103, 158, 7, (double) used / total, Ui.BLUE);
        // Hardware
        String depth = menu.stat(11) >= 99 ? "∞" : String.valueOf(menu.stat(11));
        g.text(font, Component.translatable("gui.barelystorage.hardware", menu.stat(9), menu.stat(10), depth), leftPos + 8, topPos + 41, Ui.MUTED, false);
        int jobs = menu.stat(12);
        Component line = jobs == 0 ? Component.translatable("gui.barelystorage.no_jobs")
                : Component.translatable("gui.barelystorage.jobs", jobs, menu.stat(18), menu.stat(19));
        g.text(font, line, leftPos + 8, topPos + 112 - 0, Ui.MUTED, false);
        int err = menu.stat(13);
        if (err != 0) {
            Component msg = switch (err) {
                case 1 -> Component.translatable("gui.barelystorage.error_no_core");
                case 2 -> Component.translatable("gui.barelystorage.error_threads", menu.stat(14), menu.stat(9));
                case 3 -> Component.translatable("gui.barelystorage.error_ram", menu.stat(15), menu.stat(10));
                default -> Component.translatable("gui.barelystorage.error_other");
            };
            g.fill(leftPos + 5, topPos + 58, leftPos + 171, topPos + 69, 0xFF3A1414);
            g.text(font, msg, leftPos + 8, topPos + 60, Ui.RED, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, title, titleLabelX, titleLabelY, Ui.GOLD, false);
        g.text(font, Component.translatable("gui.barelystorage.drives"), 8, 13, Ui.MUTED, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED, false);
    }
}
