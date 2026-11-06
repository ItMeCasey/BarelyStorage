package com.itmecasey.barelystorage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

/** Flat dark server-room styling drawn with plain fills, so the screens need no texture files. */
final class Ui {
    static final int NIGHT = 0xFF0F0E1A;
    static final int PANEL = 0xFF1A1830;
    static final int EDGE = 0xFF2E2A46;
    static final int GOLD = 0xFFF2B33D;
    static final int GOLD_DARK = 0xFFB27420;
    static final int TEXT = 0xFFF4F2FF;
    static final int MUTED = 0xFFA9A4C8;
    static final int RED = 0xFFFF6B5E;
    static final int GREEN = 0xFF5BE38A;
    static final int BLUE = 0xFF7AA8FF;
    static final int SLOT = 0xFF0B0A14;

    private Ui() {
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF25223D);
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
