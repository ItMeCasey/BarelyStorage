package com.itmecasey.barelystorage.client;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A small square toggle button drawn with a pixel glyph instead of text, in the mod's flat style. Each glyph has one picture per state
 * (for example three bars, two bars, one bar); a click calls the action and the tooltip names the current state.
 */
final class GlyphButton extends AbstractWidget {
    /** 9x9 pictures: '#' lit, '+' half lit, '.' empty. */
    static final String[][] HEIGHT = {
            {".........", ".........", "#########", ".........", ".........", "+++++++++", ".........", ".........", "+++++++++"},
            {".........", "#########", ".........", "#########", ".........", "+++++++++", ".........", ".........", "........."},
            {"#########", ".........", "#########", ".........", "#########", ".........", "#########", ".........", "#########"}};
    static final String[][] THEME = {
            {"...###...", ".##......", ".#.......", "##.......", "##.......", ".#.......", ".##....#.", "..#####..", "........."},
            {"....#....", ".#..#..#.", "..#####..", "..#####..", "###...###", "..#####..", "..#####..", ".#..#..#.", "....#...."}};
    static final String[][] SORT = {
            {".#..#....", ".#..#....", "#######..", ".#..#....", "#######..", ".#..#....", ".#..#....", ".........", "........."},
            {"..#####..", ".#.....#.", "#.#...#.#", "#..#.#..#", "#...#...#", "#.......#", "#.......#", ".........", "........."},
            {"....#....", "...#.#...", "..#...#..", ".#######.", ".#.....#.", "#.......#", "#.......#", ".........", "........."}};
    static final String[][] ORDER = {
            {"....#....", "...###...", "..#####..", ".#######.", "....#....", "....#....", "....#....", ".........", "........."},
            {"....#....", "....#....", "....#....", ".#######.", "..#####..", "...###...", "....#....", ".........", "........."}};
    static final String[][] VIEW = {
            {"#######..", "#.....#..", "#.....#..", "#..#######", "#..#....#.", "####....#.", "...#....#.", "...######.", "........."},
            {"#########", "#.......#", "#.#####.#", "#.#...#.#", "#.#####.#", "#.......#", "#########", ".........", "........."},
            {"..#####..", ".#.....#.", "#........", "#........", "#........", ".#.....#.", "..#####..", ".........", "........."}};

    private final String[][] glyph;
    private final IntSupplier state;
    private final Runnable action;
    private final java.util.function.Supplier<Component> tip;
    private Component lastTip;

    GlyphButton(int x, int y, String[][] glyph, IntSupplier state, Runnable action, java.util.function.Supplier<Component> tip) {
        super(x, y, 18, 18, Component.empty());
        this.glyph = glyph;
        this.state = state;
        this.action = action;
        this.tip = tip;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        action.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX();
        int y = getY();
        boolean hover = isHovered();
        g.fill(x, y, x + 18, y + 18, hover ? Ui.GOLD : Ui.EDGE);
        g.fill(x + 1, y + 1, x + 17, y + 17, hover ? Ui.PANEL : Ui.SLOT);
        String[] rows = glyph[Math.max(0, Math.min(glyph.length - 1, state.getAsInt()))];
        int color = hover ? Ui.GOLD : Ui.title();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                char ch = rows[r].charAt(c);
                if (ch == '#') {
                    g.fill(x + 4 + c, y + 4 + r, x + 5 + c, y + 5 + r, color);
                } else if (ch == '+') {
                    g.fill(x + 4 + c, y + 4 + r, x + 5 + c, y + 5 + r, (color & 0xFFFFFF) | 0x66000000);
                }
            }
        }
        Component now = tip.get();
        if (!now.equals(lastTip)) {
            lastTip = now;
            setTooltip(Tooltip.create(now));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
