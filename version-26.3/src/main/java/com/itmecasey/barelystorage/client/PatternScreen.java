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
        super(menu, inventory, title, 176, 208);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 114;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> press(PatternMenu.BTN_MODE)).bounds(leftPos + 8, topPos + 100, 66, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.barelystorage.encode"), b -> press(PatternMenu.BTN_ENCODE)).bounds(leftPos + 78, topPos + 100, 50, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.barelystorage.clear"), b -> press(PatternMenu.BTN_CLEAR)).bounds(leftPos + 132, topPos + 100, 36, 14).build());
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
        modeButton.setMessage(Component.translatable(processing ? "gui.barelystorage.mode_processing" : "gui.barelystorage.mode_crafting"));
        g.text(font, Component.translatable("gui.barelystorage.inputs"), leftPos + 30, topPos + 14, Ui.MUTED, false);
        g.text(font, Component.translatable(processing ? "gui.barelystorage.outputs" : "gui.barelystorage.result"), leftPos + 108, topPos + 14, Ui.MUTED, false);
        g.text(font, Component.translatable("gui.barelystorage.blank"), leftPos + 24, topPos + 74, Ui.MUTED, false);
        g.text(font, Component.translatable("gui.barelystorage.encoded"), leftPos + 74, topPos + 74, Ui.MUTED, false);
        if (!processing) {
            g.fill(leftPos + 123, topPos + 23, leftPos + 143, topPos + 77, 0x66000000);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, title, titleLabelX, titleLabelY, Ui.GOLD, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED, false);
    }
}
