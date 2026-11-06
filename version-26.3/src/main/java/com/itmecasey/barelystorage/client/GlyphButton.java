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
        this(x, y, 14, icon, action, tip, () -> false);
    }

    GlyphButton(int x, int y, int size, Supplier<String> icon, Runnable action, Supplier<Component> tip, BooleanSupplier lit) {
        super(x, y, size, size, Component.empty());
        this.icon = icon;
        this.action = action;
        this.tip = tip;
        this.lit = lit;
    }

    static Identifier iconId(String name) {
        return ModContent.id("textures/gui/icon/" + name + ".png");
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
        g.blit(RenderPipelines.GUI_TEXTURED, iconId(icon.get()), x + (width - 12) / 2, y + (height - 12) / 2, 0.0F, 0.0F, 12, 12, 12, 12, tint);
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
