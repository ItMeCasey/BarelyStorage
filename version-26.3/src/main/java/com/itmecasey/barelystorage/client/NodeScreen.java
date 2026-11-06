package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.Priority;
import com.itmecasey.barelystorage.menu.NodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The screen of the Assembler and the nodes: upgrade cards, patterns or filter, and for a Storage Bus its priority. */
public class NodeScreen extends AbstractContainerScreen<NodeMenu> {

    public NodeScreen(NodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.playerY + 82);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.playerY - 10;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.layout() == NodeMenu.Layout.BUS) {
            addRenderableWidget(new GlyphButton(leftPos + 156, topPos + 4, 14, () -> "priority_" + menu.priority(), () -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NodeMenu.BTN_PRIORITY);
                }
            }, () -> Component.translatable("gui.barelystorage.priority", Priority.byOrdinal(menu.priority()).label()), () -> false));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int start = menu.areaStart();
        int end = start + 45;
        for (Slot s : menu.slots) {
            if (s.index >= start && s.index < end && !s.isActive()) {
                g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF15131F);
                g.fill(leftPos + s.x + 2, topPos + s.y + 2, leftPos + s.x + 14, topPos + s.y + 14, 0xFF0A0912);
            } else {
                Ui.slot(g, leftPos + s.x, topPos + s.y);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        Ui.text(g, font, title, titleLabelX, titleLabelY, Ui.title());
        boolean tall = menu.layout() == NodeMenu.Layout.INTERFACE;
        String area = switch (menu.layout()) {
            case ASSEMBLER -> "gui.barelystorage.crafting_patterns";
            case INTERFACE -> "gui.barelystorage.processing_patterns";
            case EXPORT -> "gui.barelystorage.export_filter";
            case IMPORT -> "gui.barelystorage.import_filter";
            case BUS -> "gui.barelystorage.bus_filter";
        };
        Ui.text(g, font, Component.translatable(area), menu.areaX, menu.areaY - 10, Ui.MUTED);
        if (tall) {
            Ui.text(g, font, Component.translatable("gui.barelystorage.buffer"), menu.bufferX, menu.bufferY - 10, Ui.MUTED);
        }
        Ui.text(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED);
    }
}
