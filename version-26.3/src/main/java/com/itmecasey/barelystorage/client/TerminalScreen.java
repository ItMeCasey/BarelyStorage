package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
 * The Storage Terminal: a scrollable, searchable, sortable list of everything in the network. A hamburger button on the left opens
 * a settings window (height, theme, sort, view). Items you can craft but have none of show an outline and a "C"; clicking one opens
 * the craft request window.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int GOLD = 0xFFF2B33D;
    private static final int WIN_W = 106;
    private static final int TRACK_W = 6;

    private EditBox search;
    private String sent = "";
    private Button hamburger;
    private boolean settingsOpen;
    private int tab;
    private final List<Button> displayButtons = new ArrayList<>();
    private final List<Button> viewButtons = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private final Button[] heightButtons = new Button[3];
    private final Button[] themeButtons = new Button[2];
    private final Button[] sortButtons = new Button[3];
    private final Button[] orderButtons = new Button[2];
    private final Button[] viewModeButtons = new Button[3];
    // Craft request window
    private int requestSlot = -1;
    private int amount = 1;
    private final List<Button> requestButtons = new ArrayList<>();
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
        displayButtons.clear();
        viewButtons.clear();
        tabButtons.clear();
        requestButtons.clear();
        search = new EditBox(font, leftPos + 9, topPos + 17, 166, 12, Component.translatable("gui.barelystorage.search"));
        search.setMaxLength(40);
        search.addFormatter((text, offset) -> FormattedCharSequence.forward(text, Ui.STYLE));
        search.setResponder(this::searchChanged);
        search.setFocused(true);
        addRenderableWidget(search);
        setInitialFocus(search);
        hamburger = addRenderableWidget(Button.builder(Ui.t("≡"), b -> {
            settingsOpen = !settingsOpen;
            updateVisibility();
        }).bounds(leftPos - 19, topPos + 4, 15, 15).build());
        buildSettings();
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

    private Button button(Component text, int x, int y, int w, int h, Button.OnPress press) {
        return addRenderableWidget(Button.builder(Ui.t(text), press).bounds(x, y, w, h).build());
    }

    private void buildSettings() {
        int x0 = leftPos - WIN_W - 24;
        int y0 = topPos + 4;
        tabButtons.add(button(Component.translatable("gui.barelystorage.tab_display"), x0 + 5, y0 + 5, 46, 14, b -> {
            tab = 0;
            updateVisibility();
        }));
        tabButtons.add(button(Component.translatable("gui.barelystorage.tab_view"), x0 + 55, y0 + 5, 46, 14, b -> {
            tab = 1;
            updateVisibility();
        }));
        String[] heights = {"S", "M", "L"};
        for (int i = 0; i < 3; i++) {
            final int v = i;
            heightButtons[i] = button(Component.literal(heights[i]), x0 + 5 + i * 32, y0 + 38, 30, 14, b -> {
                ClientSettings.height = v;
                ClientSettings.save();
                press(TerminalMenu.BTN_HEIGHT + v);
            });
            displayButtons.add(heightButtons[i]);
        }
        for (int i = 0; i < 2; i++) {
            final int v = i;
            themeButtons[i] = button(Component.translatable(i == 0 ? "gui.barelystorage.theme_dark" : "gui.barelystorage.theme_light"), x0 + 5 + i * 48, y0 + 68, 46, 14, b -> {
                ClientSettings.theme = v;
                ClientSettings.save();
                Ui.theme(v == 1);
            });
            displayButtons.add(themeButtons[i]);
        }
        String[] sorts = {"gui.barelystorage.sort_quantity", "gui.barelystorage.sort_mod", "gui.barelystorage.sort_name"};
        for (int i = 0; i < 3; i++) {
            final int v = i;
            sortButtons[i] = button(Component.translatable(sorts[i]), x0 + 5 + i * 32, y0 + 98, 30, 14, b -> {
                ClientSettings.sort = v;
                ClientSettings.save();
                press(TerminalMenu.BTN_SORT + v);
            });
            displayButtons.add(sortButtons[i]);
        }
        for (int i = 0; i < 2; i++) {
            final int v = i;
            orderButtons[i] = button(Component.translatable(i == 0 ? "gui.barelystorage.order_asc" : "gui.barelystorage.order_desc"), x0 + 5 + i * 48, y0 + 128, 46, 14, b -> {
                ClientSettings.order = v;
                ClientSettings.save();
                press(TerminalMenu.BTN_ORDER + v);
            });
            displayButtons.add(orderButtons[i]);
        }
        String[] views = {"gui.barelystorage.view_both", "gui.barelystorage.view_available", "gui.barelystorage.view_craftable"};
        for (int i = 0; i < 3; i++) {
            final int v = i;
            viewModeButtons[i] = button(Component.translatable(views[i]), x0 + 5, y0 + 28 + i * 20, 96, 16, b -> {
                ClientSettings.view = v;
                ClientSettings.save();
                press(TerminalMenu.BTN_VIEW + v);
            });
            viewButtons.add(viewModeButtons[i]);
        }
    }

    private void buildRequest() {
        int x = leftPos + 27;
        int y = topPos + 52;
        int[] deltas = {-100, -10, -1, 1, 10, 100};
        for (int i = 0; i < 6; i++) {
            final int d = deltas[i];
            requestButtons.add(button(Component.literal((d > 0 ? "+" : "") + d), x + 4 + i * 21, y + 38, 20, 14, b -> amount = Math.max(1, Math.min(9999, amount + d))));
        }
        requestButtons.add(button(Component.translatable("gui.barelystorage.start"), x + 4, y + 58, 60, 16, b -> {
            if (requestSlot >= 0) {
                press(TerminalMenu.BTN_CRAFT + requestSlot * 10000 + amount);
            }
            closeRequest();
        }));
        requestButtons.add(button(Component.translatable("gui.barelystorage.cancel"), x + 66, y + 58, 60, 16, b -> closeRequest()));
    }

    private void closeRequest() {
        requestSlot = -1;
        updateVisibility();
    }

    private void updateVisibility() {
        for (Button b : tabButtons) {
            b.visible = settingsOpen;
            b.active = settingsOpen;
        }
        for (Button b : displayButtons) {
            b.visible = settingsOpen && tab == 0;
            b.active = b.visible;
        }
        for (Button b : viewButtons) {
            b.visible = settingsOpen && tab == 1;
            b.active = b.visible;
        }
        for (Button b : requestButtons) {
            b.visible = requestSlot >= 0;
            b.active = b.visible;
        }
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

    private Component pick(Component text, boolean selected) {
        return selected ? Ui.t(text, GOLD) : Ui.t(text);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.slots(g, menu.slots, leftPos, topPos);
        boolean online = menu.online();
        g.fill(leftPos + 166, topPos + 5, leftPos + 176, topPos + 9, online ? Ui.GREEN : Ui.RED);
        // Scrollbar
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
        if (settingsOpen) {
            drawSettings(g);
        }
        for (int i = 0; i < 3; i++) {
            heightButtons[i].setMessage(pick(Component.literal(new String[] {"S", "M", "L"}[i]), ClientSettings.height == i));
            sortButtons[i].setMessage(pick(Component.translatable(new String[] {"gui.barelystorage.sort_quantity", "gui.barelystorage.sort_mod", "gui.barelystorage.sort_name"}[i]), ClientSettings.sort == i));
            viewModeButtons[i].setMessage(pick(Component.translatable(new String[] {"gui.barelystorage.view_both", "gui.barelystorage.view_available", "gui.barelystorage.view_craftable"}[i]), ClientSettings.view == i));
        }
        for (int i = 0; i < 2; i++) {
            themeButtons[i].setMessage(pick(Component.translatable(i == 0 ? "gui.barelystorage.theme_dark" : "gui.barelystorage.theme_light"), ClientSettings.theme == i));
            orderButtons[i].setMessage(pick(Component.translatable(i == 0 ? "gui.barelystorage.order_asc" : "gui.barelystorage.order_desc"), ClientSettings.order == i));
        }
        tabButtons.get(0).setMessage(pick(Component.translatable("gui.barelystorage.tab_display"), tab == 0));
        tabButtons.get(1).setMessage(pick(Component.translatable("gui.barelystorage.tab_view"), tab == 1));
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int x0 = leftPos - WIN_W - 24;
        int y0 = topPos + 4;
        Ui.panel(g, x0, y0, WIN_W, tab == 0 ? 148 : 92);
        if (tab == 0) {
            Ui.text(g, font, Component.translatable("gui.barelystorage.height"), x0 + 6, y0 + 27, Ui.MUTED);
            Ui.text(g, font, Component.translatable("gui.barelystorage.theme"), x0 + 6, y0 + 57, Ui.MUTED);
            Ui.text(g, font, Component.translatable("gui.barelystorage.sort_by"), x0 + 6, y0 + 87, Ui.MUTED);
            Ui.text(g, font, Component.translatable("gui.barelystorage.order"), x0 + 6, y0 + 117, Ui.MUTED);
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
            String text = craftable ? "C" : stack.getCount() > 1 ? CartridgeItem.fmt(stack.getCount()) : "";
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
            drawRequest(g);
        }
    }

    private void drawRequest(GuiGraphicsExtractor g) {
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
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (requestSlot < 0 && hoveredSlot != null && hoveredSlot.index < menu.viewSlots() && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            ItemStack s = hoveredSlot.getItem();
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(s));
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
