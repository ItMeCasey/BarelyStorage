package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.menu.PatternMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Pattern Terminal: a recipe grid and the buttons that encode it onto a blank pattern. */
public class PatternScreen extends AbstractContainerScreen<PatternMenu> {
    private Button modeButton;

    public PatternScreen(PatternMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 216);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 124;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(Ui.t(Component.empty()), b -> press(PatternMenu.BTN_MODE)).bounds(leftPos + 8, topPos + 108, 66, 14).build());
        addRenderableWidget(Button.builder(Ui.t(Component.translatable("gui.barelystorage.encode")), b -> press(PatternMenu.BTN_ENCODE)).bounds(leftPos + 78, topPos + 108, 50, 14).build());
        addRenderableWidget(Button.builder(Ui.t(Component.translatable("gui.barelystorage.clear")), b -> press(PatternMenu.BTN_CLEAR)).bounds(leftPos + 132, topPos + 108, 36, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.slots(g, menu.slots, leftPos, topPos);
        boolean processing = menu.processing();
        modeButton.setMessage(Ui.t(Component.translatable(processing ? "gui.barelystorage.mode_processing" : "gui.barelystorage.mode_crafting")));
        Ui.text(g, font, Component.translatable("gui.barelystorage.inputs"), leftPos + 30, topPos + 15, Ui.MUTED);
        Ui.text(g, font, Component.translatable("gui.barelystorage.outputs"), leftPos + 92, topPos + 15, Ui.MUTED);
        Ui.text(g, font, Component.translatable("gui.barelystorage.result"), leftPos + 136, topPos + 31, Ui.MUTED);
        Ui.text(g, font, "→", leftPos + 118, topPos + 46, Ui.GOLD);
        Ui.text(g, font, Component.translatable("gui.barelystorage.blank"), leftPos + 28, topPos + 80, Ui.MUTED);
        Ui.text(g, font, Component.translatable("gui.barelystorage.encoded"), leftPos + 80, topPos + 80, Ui.MUTED);
        if (processing) {
            g.fill(leftPos + 139, topPos + 41, leftPos + 157, topPos + 59, 0xAA0B0A14);
        } else {
            g.fill(leftPos + 95, topPos + 23, leftPos + 113, topPos + 77, 0xAA0B0A14);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        Ui.text(g, font, title, titleLabelX, titleLabelY, Ui.title());
        Ui.text(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED);
    }
}
