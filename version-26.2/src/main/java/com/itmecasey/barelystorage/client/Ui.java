package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.ModContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.Slot;

/**
 * Flat dark server-room styling drawn with plain fills, so the screens need no texture files, and the mod's text font: Liberation
 * Sans (SIL Open Font License) as a TrueType font, so text stays sharp and readable at small sizes and when scaled.
 */
final class Ui {
    static final FontDescription FONT = new FontDescription.Resource(ModContent.id("ui"));
    static final Style STYLE = Style.EMPTY.withFont(FONT);
    /** Silkscreen (SIL Open Font License): the pixel font for titles, labels and buttons; numbers and names stay in the UI font. */
    static final FontDescription FONT_SILK = new FontDescription.Resource(ModContent.id("silk"));
    static final Style STYLE_SILK = Style.EMPTY.withFont(FONT_SILK);
    static final int NIGHT = 0xFF0F0E1A;
    static int PANEL = 0xFF1A1830;
    static int EDGE = 0xFF2E2A46;
    static final int GOLD = 0xFFF2B33D;
    static final int GOLD_DARK = 0xFFB27420;
    static int TEXT = 0xFFF4F2FF;
    static int MUTED = 0xFFA9A4C8;
    static final int RED = 0xFFFF6B5E;
    static final int GREEN = 0xFF5BE38A;
    static final int BLUE = 0xFF7AA8FF;
    static int SLOT = 0xFF0B0A14;

    private Ui() {
    }

    private static int titleColor = 0xFFF2B33D;
    private static int highlight = 0xFF25223D;

    /** Switches every screen between the dark and the light look. */
    static void theme(boolean light) {
        if (light) {
            PANEL = 0xFFD8D8E4;
            EDGE = 0xFF8C8CA6;
            SLOT = 0xFFB4B4C6;
            TEXT = 0xFF1A1830;
            MUTED = 0xFF4A4A66;
            titleColor = 0xFF8A5A10;
            highlight = 0xFFEAEAF2;
        } else {
            PANEL = 0xFF1A1830;
            EDGE = 0xFF2E2A46;
            SLOT = 0xFF0B0A14;
            TEXT = 0xFFF4F2FF;
            MUTED = 0xFFA9A4C8;
            titleColor = 0xFFF2B33D;
            highlight = 0xFF25223D;
        }
    }

    static int title() {
        return titleColor;
    }

    static Component t(Component c, int rgb) {
        return c.copy().withStyle(STYLE).withColor(rgb & 0xFFFFFF);
    }

    /** The component in the mod font. */
    static Component t(Component c) {
        return c.copy().withStyle(STYLE);
    }

    static Component t(String s) {
        return Component.literal(s).withStyle(STYLE);
    }

    /** A title or label in the pixel font. */
    static void label(GuiGraphicsExtractor g, Font font, Component c, int x, int y, int color) {
        g.text(font, c.copy().withStyle(STYLE_SILK), x, y, color, false);
    }

    static void text(GuiGraphicsExtractor g, Font font, Component c, int x, int y, int color) {
        g.text(font, t(c), x, y, color, false);
    }

    static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, t(s), x, y, color, false);
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, highlight);
        g.fill(x, y, x + 3, y + 1, GOLD);
        g.fill(x, y, x + 1, y + 3, GOLD);
    }

    static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, EDGE);
        g.fill(x, y, x + 16, y + 16, SLOT);
    }

    static void slots(GuiGraphicsExtractor g, Iterable<Slot> slots, int left, int top) {
        for (Slot s : slots) {
            slot(g, left + s.x, top + s.y);
        }
    }

    static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double frac, int color) {
        g.fill(x, y, x + w, y + h, SLOT);
        g.fill(x, y, x + w, y + 1, EDGE);
        int fill = (int) Math.round((w - 2) * Math.max(0, Math.min(1, frac)));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x55FFFFFF);
        }
    }
}
