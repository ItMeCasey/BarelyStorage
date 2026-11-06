package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.ModContent;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A small square button that shows only an icon (a smooth 12x12 sprite tinted for the theme); the tooltip says what it does. A button
 * can be "lit" to show that a toggle is on. The icon can change with the button's state.
 */
final class GlyphButton extends AbstractWidget {
    private final Supplier<String> icon;
    private final Runnable action;
    private final Supplier<Component> tip;
    private final BooleanSupplier lit;
    private Component lastTip;

    GlyphButton(int x, int y, Supplier<String> icon, Runnable action, Supplier<Component> tip) {
        this(x, y, 12, icon, action, tip, () -> false);
    }

    GlyphButton(int x, int y, int size, Supplier<String> icon, Runnable action, Supplier<Component> tip, BooleanSupplier lit) {
        super(x, y, size, size, Component.empty());
        this.icon = icon;
        this.action = action;
        this.tip = tip;
        this.lit = lit;
    }

    /** The sprite for the current GUI scale (1 to 5), drawn so one texture pixel is one screen pixel. */
    static Identifier iconId(String name) {
        int scale = Math.max(1, Math.min(5, (int) Math.round(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale())));
        return ModContent.id("textures/gui/icon/" + name + "_s" + scale + ".png");
    }

    static int iconPixels() {
        return 9 * Math.max(1, Math.min(5, (int) Math.round(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale())));
    }

    static void drawIcon(GuiGraphicsExtractor g, String name, int x, int y, int tint) {
        int px = iconPixels();
        g.blit(RenderPipelines.GUI_TEXTURED, iconId(name), x, y, 0.0F, 0.0F, 9, 9, px, px, px, px, tint);
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
        boolean on = lit.getAsBoolean();
        int border = hover ? Ui.GOLD : on ? Ui.GOLD_DARK : Ui.EDGE;
        g.fill(x, y, x + width, y + height, border);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, hover ? Ui.PANEL : Ui.SLOT);
        int tint = hover || on ? Ui.GOLD : Ui.TEXT;
        drawIcon(g, icon.get(), x + (width - 9) / 2, y + (height - 9) / 2, tint);
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
