package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Storage Terminal: a scrollable, searchable, sortable list of everything in the network. A column of glyph buttons on its left
 * switches height, theme, sort, order and view. Items you can craft but have none of show an outline and a "C"; clicking one opens
 * the craft request window.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int GOLD = 0xFFF2B33D;
    private static final int TRACK_W = 6;

    private EditBox search;
    private boolean syncSearch;
    private String lastViewer = "";
    private String lastTerminal = "";
    private String sent = "";
    // Craft request window
    private int requestSlot = -1;
    private int amount = 1;
    private final List<AbstractWidget> requestButtons = new ArrayList<>();
    private boolean draggingScroll;
    private int lastScrollSent = -1;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 184, menu.playerY + 82);
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.playerY - 10;
    }

    @Override
    protected void init() {
        super.init();
        ClientSettings.load();
        requestButtons.clear();
        search = new EditBox(font, leftPos + 9, topPos + 17, 166, 12, Component.translatable("gui.barelystorage.search"));
        search.setMaxLength(40);
        search.addFormatter((text, offset) -> FormattedCharSequence.forward(text, Ui.STYLE));
        search.setResponder(this::searchChanged);
        search.setFocused(true);
        addRenderableWidget(search);
        setInitialFocus(search);
        buildGlyphs();
        buildExtras();
        buildRequest();
        updateVisibility();
        // Bring the server in line with the saved choices.
        int rowsIndex = 0;
        for (int i = 0; i < TerminalMenu.ROWS.length; i++) {
            if (TerminalMenu.ROWS[i] == menu.rows()) {
                rowsIndex = i;
            }
        }
        if (ClientSettings.height != rowsIndex) {
            press(TerminalMenu.BTN_HEIGHT + ClientSettings.height);
            return;
        }
        if (ClientSettings.sort != 0) {
            press(TerminalMenu.BTN_SORT + ClientSettings.sort);
        }
        if (ClientSettings.order != 0) {
            press(TerminalMenu.BTN_ORDER + ClientSettings.order);
        }
        if (ClientSettings.view != 0) {
            press(TerminalMenu.BTN_VIEW + ClientSettings.view);
        }
    }

    private GlyphButton add(GlyphButton b) {
        return addRenderableWidget(b);
    }

    /** The glyph column: height, theme, sort method, order, view. Each click moves to the next state. */
    private void buildGlyphs() {
        int x = leftPos - 20;
        int y = topPos + 5;
        add(new GlyphButton(x, y, () -> "height_" + ClientSettings.height, () -> {
            ClientSettings.height = (ClientSettings.height + 1) % 3;
            ClientSettings.save();
            press(TerminalMenu.BTN_HEIGHT + ClientSettings.height);
        }, () -> Component.translatable("gui.barelystorage.tip_height", Component.translatable("gui.barelystorage.size_" + ClientSettings.height))));
        add(new GlyphButton(x, y + 18, () -> "theme_" + ClientSettings.theme, () -> {
            ClientSettings.theme = 1 - ClientSettings.theme;
            ClientSettings.save();
            Ui.theme(ClientSettings.theme == 1);
        }, () -> Component.translatable("gui.barelystorage.tip_theme", Component.translatable(ClientSettings.theme == 0 ? "gui.barelystorage.theme_dark" : "gui.barelystorage.theme_light"))));
        add(new GlyphButton(x, y + 36, () -> "sort_" + ClientSettings.sort, () -> {
            ClientSettings.sort = (ClientSettings.sort + 1) % 3;
            ClientSettings.save();
            press(TerminalMenu.BTN_SORT + ClientSettings.sort);
        }, () -> Component.translatable("gui.barelystorage.tip_sort", Component.translatable("gui.barelystorage.sort_full_" + ClientSettings.sort))));
        add(new GlyphButton(x, y + 54, () -> "order_" + ClientSettings.order, () -> {
            ClientSettings.order = 1 - ClientSettings.order;
            ClientSettings.save();
            press(TerminalMenu.BTN_ORDER + ClientSettings.order);
        }, () -> Component.translatable("gui.barelystorage.tip_order", Component.translatable(ClientSettings.order == 0 ? "gui.barelystorage.order_asc" : "gui.barelystorage.order_desc"))));
        add(new GlyphButton(x, y + 72, () -> "view_" + ClientSettings.view, () -> {
            ClientSettings.view = (ClientSettings.view + 1) % 3;
            ClientSettings.save();
            press(TerminalMenu.BTN_VIEW + ClientSettings.view);
        }, () -> Component.translatable("gui.barelystorage.tip_view", Component.translatable(new String[] {"gui.barelystorage.view_both",
                "gui.barelystorage.view_available", "gui.barelystorage.view_craftable"}[ClientSettings.view]))));
        if (SearchSync.available()) {
            add(new GlyphButton(x, y + 90, 16, () -> "sync", () -> {
                syncSearch = !syncSearch;
                lastViewer = SearchSync.get();
                lastTerminal = search.getValue();
                if (syncSearch) {
                    SearchSync.set(lastTerminal);
                }
            }, () -> Component.translatable("gui.barelystorage.tip_sync"), () -> syncSearch));
        }
    }

    /** Icon buttons under the item list: clear for the Crafting Terminal; mode, tags, encode and clear for the Pattern Terminal. */
    private void buildExtras() {
        int y = topPos + menu.extrasY;
        if (menu.kind() == TerminalMenu.Kind.CRAFTING) {
            add(new GlyphButton(leftPos + 150, y + 40, () -> "clear", () -> press(TerminalMenu.BTN_GRID_CLEAR), () -> Component.translatable("gui.barelystorage.clear")));
        } else if (menu.kind() == TerminalMenu.Kind.PATTERN) {
            int row = y + 64;
            add(new GlyphButton(leftPos + 8, row, 16, () -> "mode_" + (menu.processing() ? 1 : 0), () -> press(TerminalMenu.BTN_MODE),
                    () -> Component.translatable(menu.processing() ? "gui.barelystorage.mode_processing" : "gui.barelystorage.mode_crafting"), () -> false));
            add(new GlyphButton(leftPos + 26, row, 16, () -> "tags", () -> press(TerminalMenu.BTN_TAGS),
                    () -> Component.translatable("gui.barelystorage.tags_tip"), () -> menu.tags()));
            add(new GlyphButton(leftPos + 44, row, 16, () -> "encode", () -> press(TerminalMenu.BTN_ENCODE),
                    () -> Component.translatable("gui.barelystorage.encode"), () -> false));
            add(new GlyphButton(leftPos + 62, row, 16, () -> "clear", () -> press(TerminalMenu.BTN_GRID_CLEAR),
                    () -> Component.translatable("gui.barelystorage.clear"), () -> false));
        }
    }

    private void drawExtras(GuiGraphicsExtractor g) {
        if (menu.kind() == TerminalMenu.Kind.STORAGE) {
            return;
        }
        int y = topPos + menu.extrasY;
        if (menu.kind() == TerminalMenu.Kind.CRAFTING) {
            Ui.text(g, font, "\u2192", leftPos + 100, y + 22, GOLD);
            for (int i = 0; i < TerminalMenu.GRID; i++) {
                if (menu.data(menu.dMissing(i)) != 0) {
                    Slot s = menu.slots.get(menu.viewSlots() + i);
                    g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0x99FF3030);
                    g.outline(leftPos + s.x - 1, topPos + s.y - 1, 18, 18, Ui.RED);
                }
            }
            if (menu.data(menu.dResult()) == 2) {
                g.fill(leftPos + 124, y + 18, leftPos + 140, y + 34, 0x99FF3030);
            }
        } else {
            boolean processing = menu.processing();
            Ui.text(g, font, "\u2192", leftPos + 90, y + 4, GOLD);
            if (processing) {
                g.fill(leftPos + 103, y + 17, leftPos + 121, y + 35, 0xAA0B0A14);
            } else {
                g.fill(leftPos + 77, y - 1, leftPos + 95, y + 53, 0xAA0B0A14);
            }
        }
    }

    private void buildRequest() {
        int x = leftPos + 27;
        int y = topPos + 52;
        String[] names = {"minus_100", "minus_10", "minus_1", "plus_1", "plus_10", "plus_100"};
        int[] deltas = {-100, -10, -1, 1, 10, 100};
        for (int i = 0; i < 6; i++) {
            final int d = deltas[i];
            requestButtons.add(add(new GlyphButton(x + 8 + i * 19, y + 38, 18, () -> names[Math.max(0, Math.min(5, java.util.Arrays.binarySearch(deltas, d)))],
                    () -> amount = Math.max(1, Math.min(9999, amount + d)),
                    () -> Component.translatable(d > 0 ? "gui.barelystorage.tip_add" : "gui.barelystorage.tip_remove", Math.abs(d)), () -> false)));
        }
        requestButtons.add(add(new GlyphButton(x + 30, y + 58, 20, () -> "start", () -> {
            if (requestSlot >= 0) {
                press(TerminalMenu.BTN_CRAFT + requestSlot * 10000 + amount);
            }
            closeRequest();
        }, () -> Component.translatable("gui.barelystorage.start"), () -> false)));
        requestButtons.add(add(new GlyphButton(x + 82, y + 58, 20, () -> "cancel", this::closeRequest, () -> Component.translatable("gui.barelystorage.cancel"), () -> false)));
    }

    private void closeRequest() {
        requestSlot = -1;
        updateVisibility();
    }

    private void updateVisibility() {
        for (AbstractWidget b : requestButtons) {
            b.visible = requestSlot >= 0;
            b.active = b.visible;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!syncSearch || !SearchSync.available()) {
            return;
        }
        String viewer = SearchSync.get();
        String mine = search.getValue();
        if (!viewer.equals(lastViewer) && !viewer.equals(mine)) {
            search.setValue(viewer.length() > 40 ? viewer.substring(0, 40) : viewer);
        } else if (!mine.equals(lastTerminal) && !mine.equals(viewer)) {
            SearchSync.set(mine);
        }
        lastViewer = SearchSync.get();
        lastTerminal = search.getValue();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

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

    // ---- Input ----

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (requestSlot >= 0) {
            return;
        }
        if (slot != null && slot.index < menu.viewSlots() && menu.craftableAt(slot.index) && menu.getCarried().isEmpty()
                && containerInput == ContainerInput.PICKUP && buttonNum == 0) {
            requestSlot = slot.index;
            amount = 1;
            updateVisibility();
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    private int trackX() {
        return leftPos + TerminalMenu.LIST_X + 162 + 4;
    }

    private int trackY() {
        return topPos + TerminalMenu.LIST_Y;
    }

    private int trackH() {
        return menu.rows() * 18;
    }

    private void scrollTo(int row) {
        row = Math.max(0, Math.min(menu.maxScroll(), row));
        if (row != lastScrollSent && row != menu.scroll()) {
            lastScrollSent = row;
            press(TerminalMenu.BTN_SCROLL + row);
        }
    }

    private void scrollFromMouse(double mouseY) {
        int total = menu.rows() + menu.maxScroll();
        int handle = Math.max(14, trackH() * menu.rows() / Math.max(1, total));
        double frac = (mouseY - trackY() - handle / 2.0) / Math.max(1, trackH() - handle);
        scrollTo((int) Math.round(Math.max(0, Math.min(1, frac)) * menu.maxScroll()));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (requestSlot < 0 && x >= leftPos && x < leftPos + imageWidth && y >= topPos + TerminalMenu.LIST_Y && y < topPos + TerminalMenu.LIST_Y + trackH()) {
            scrollTo(menu.scroll() + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (requestSlot < 0 && menu.maxScroll() > 0 && event.x() >= trackX() && event.x() < trackX() + TRACK_W
                && event.y() >= trackY() && event.y() < trackY() + trackH()) {
            draggingScroll = true;
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScroll) {
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    // ---- Drawing ----

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.slots(g, menu.slots, leftPos, topPos);
        boolean online = menu.online();
        g.fill(leftPos + 166, topPos + 5, leftPos + 176, topPos + 9, online ? Ui.GREEN : Ui.RED);
        drawExtras(g);
        g.fill(trackX(), trackY(), trackX() + TRACK_W, trackY() + trackH(), Ui.SLOT);
        if (menu.maxScroll() > 0) {
            int total = menu.rows() + menu.maxScroll();
            int handle = Math.max(14, trackH() * menu.rows() / total);
            int hy = trackY() + (trackH() - handle) * menu.scroll() / menu.maxScroll();
            g.fill(trackX(), hy, trackX() + TRACK_W, hy + handle, Ui.EDGE);
            g.fill(trackX() + 1, hy + 1, trackX() + TRACK_W - 1, hy + handle - 1, GOLD);
        }
        if (!online) {
            Component off = Ui.t(Component.translatable("gui.barelystorage.offline"));
            g.text(font, off, leftPos + 92 - font.width(off) / 2, topPos + 80, Ui.RED, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        Ui.text(g, font, title, titleLabelX, titleLabelY, Ui.title());
        Ui.text(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mx, int my) {
        if (slot.index < menu.viewSlots()) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                return;
            }
            boolean craftable = menu.craftableAt(slot.index);
            g.item(stack, slot.x, slot.y, slot.x + slot.y * imageWidth);
            if (craftable) {
                g.outline(slot.x - 1, slot.y - 1, 18, 18, GOLD);
            }
            String text = craftable ? "C" : menu.countAt(slot.index) > 1 ? CartridgeItem.fmt(menu.countAt(slot.index)) : "";
            if (!text.isEmpty()) {
                Component c = Ui.t(text);
                g.nextStratum();
                g.pose().pushMatrix();
                g.pose().translate(slot.x + 17 - font.width(c) * 0.75F, slot.y + 9);
                g.pose().scale(0.75F, 0.75F);
                g.text(font, c, 0, 0, craftable ? GOLD : 0xFFFFFFFF, true);
                g.pose().popMatrix();
            }
            return;
        }
        super.extractSlot(g, slot, mx, my);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (requestSlot >= 0) {
            drawRequest(g, mouseX, mouseY, a);
        }
    }

    private void drawRequest(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.nextStratum();
        int x = leftPos + 27;
        int y = topPos + 52;
        Ui.panel(g, x, y, 130, 80);
        ItemStack stack = menu.slots.get(requestSlot).getItem();
        g.item(stack, x + 6, y + 6);
        Ui.text(g, font, Component.translatable("gui.barelystorage.craft_title"), x + 28, y + 6, Ui.title());
        Component name = Ui.t(stack.getHoverName());
        g.text(font, name, x + 28, y + 17, Ui.TEXT, false);
        Component count = Ui.t(Component.translatable("gui.barelystorage.craft_amount", amount));
        g.text(font, count, x + 65 - font.width(count) / 2, y + 28, GOLD, false);
        for (AbstractWidget b : requestButtons) {
            b.extractRenderState(g, mouseX, mouseY, a);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (requestSlot < 0 && hoveredSlot != null && hoveredSlot.index < menu.viewSlots() && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            ItemStack s = hoveredSlot.getItem();
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(s));
            if (menu.craftableAt(hoveredSlot.index)) {
                lines.add(Component.translatable("gui.barelystorage.craftable"));
            } else {
                lines.add(Component.translatable("gui.barelystorage.stored", menu.countAt(hoveredSlot.index)));
            }
            g.setComponentTooltipForNextFrame(font, lines, mx, my);
            return;
        }
        super.extractTooltip(g, mx, my);
    }
}
