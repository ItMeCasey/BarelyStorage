package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Storage Terminal: a searchable, sortable list of everything in the network. */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private EditBox search;
    private String sent = "";
    private Button sortButton;
    private Button amountButton;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 234);
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 9, topPos + 17, 96, 12, Component.translatable("gui.barelystorage.search"));
        search.setMaxLength(40);
        search.addFormatter((text, offset) -> FormattedCharSequence.forward(text, Ui.STYLE));
        search.setResponder(this::searchChanged);
        search.setFocused(true);
        addRenderableWidget(search);
        setInitialFocus(search);
        sortButton = addRenderableWidget(Button.builder(Ui.t("A-Z"), b -> press(TerminalMenu.BTN_SORT)).bounds(leftPos + 110, topPos + 15, 28, 16).build());
        amountButton = addRenderableWidget(Button.builder(Ui.t("x1"), b -> press(TerminalMenu.BTN_AMOUNT)).bounds(leftPos + 140, topPos + 15, 28, 16).build());
        addRenderableWidget(Button.builder(Ui.t("<"), b -> press(TerminalMenu.BTN_PREV)).bounds(leftPos + 8, topPos + 128, 18, 12).build());
        addRenderableWidget(Button.builder(Ui.t(">"), b -> press(TerminalMenu.BTN_NEXT)).bounds(leftPos + 150, topPos + 128, 18, 12).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** Keeps the server's copy of the search text in step by sending only what changed. */
    private void searchChanged(String text) {
        if (text.startsWith(sent)) {
            for (int i = sent.length(); i < text.length(); i++) {
                press(TerminalMenu.BTN_CHAR + text.charAt(i));
            }
        } else if (sent.startsWith(text) && sent.length() - text.length() <= 3) {
            for (int i = text.length(); i < sent.length(); i++) {
                press(TerminalMenu.BTN_BACKSPACE);
            }
        } else {
            press(TerminalMenu.BTN_CLEAR);
            for (int i = 0; i < text.length(); i++) {
                press(TerminalMenu.BTN_CHAR + text.charAt(i));
            }
        }
        sent = text;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.slots(g, menu.slots, leftPos, topPos);
        sortButton.setMessage(Ui.t(menu.data(TerminalMenu.D_SORT) == 0 ? "A-Z" : "123"));
        amountButton.setMessage(Ui.t("x" + TerminalMenu.AMOUNTS[Math.min(2, menu.data(TerminalMenu.D_AMOUNT))]));
        boolean online = menu.data(TerminalMenu.D_ONLINE) != 0;
        g.fill(leftPos + 158, topPos + 5, leftPos + 168, topPos + 9, online ? Ui.GREEN : Ui.RED);
        int page = menu.data(TerminalMenu.D_PAGE) + 1;
        int max = menu.data(TerminalMenu.D_MAXPAGE) + 1;
        Component pageText = Ui.t(Component.translatable("gui.barelystorage.page", page, max));
        g.text(font, pageText, leftPos + 88 - font.width(pageText) / 2, topPos + 130, Ui.MUTED, false);
        if (!online) {
            Component off = Ui.t(Component.translatable("gui.barelystorage.offline"));
            g.text(font, off, leftPos + 88 - font.width(off) / 2, topPos + 78, Ui.RED, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        Ui.text(g, font, title, titleLabelX, titleLabelY, Ui.GOLD);
        Ui.text(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mx, int my) {
        if (slot.index < TerminalMenu.VIEW) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                return;
            }
            boolean craftable = menu.craftableAt(slot.index);
            g.item(stack, slot.x, slot.y, slot.x + slot.y * imageWidth);
            if (craftable) {
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x66101018);
            }
            String text = craftable ? "C" : stack.getCount() > 1 ? CartridgeItem.fmt(stack.getCount()) : "";
            if (!text.isEmpty()) {
                Component c = Ui.t(text);
                g.nextStratum();
                g.pose().pushMatrix();
                g.pose().translate(slot.x + 17 - font.width(c) * 0.75F, slot.y + 9);
                g.pose().scale(0.75F, 0.75F);
                g.text(font, c, 0, 0, craftable ? Ui.GOLD : Ui.TEXT, true);
                g.pose().popMatrix();
            }
            return;
        }
        super.extractSlot(g, slot, mx, my);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (hoveredSlot != null && hoveredSlot.index < TerminalMenu.VIEW && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            ItemStack s = hoveredSlot.getItem();
            java.util.List<Component> lines = new java.util.ArrayList<>(getTooltipFromContainerItem(s));
            if (menu.craftableAt(hoveredSlot.index)) {
                lines.add(Component.translatable("gui.barelystorage.craftable"));
            } else {
                lines.add(Component.translatable("gui.barelystorage.stored", s.getCount()));
            }
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
            return;
        }
        super.extractTooltip(g, mx, my);
    }
}
