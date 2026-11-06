package com.itmecasey.barelystorage.client;

import com.itmecasey.barelystorage.menu.QueueMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Crafting Queue Terminal: each job with its progress bar, state and a cancel button, and the halt error when the queue is stuck. */
public class QueueScreen extends AbstractContainerScreen<QueueMenu> {
    private final List<Button> cancels = new ArrayList<>();

    public QueueScreen(QueueMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.playerY() + 82);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.playerY() - 10;
    }

    @Override
    protected void init() {
        super.init();
        cancels.clear();
        for (int i = 0; i < QueueMenu.JOBS; i++) {
            final int job = i;
            Button b = Button.builder(Ui.t("✕"), x -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, job);
                }
            }).bounds(leftPos + 154, topPos + 30 + i * 20, 14, 16).tooltip(Tooltip.create(Component.translatable("gui.barelystorage.queue_cancel"))).build();
            cancels.add(addRenderableWidget(b));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Ui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.slots(g, menu.slots, leftPos, topPos);
        boolean any = false;
        for (int i = 0; i < QueueMenu.JOBS; i++) {
            int state = menu.data(i * QueueMenu.PER_JOB);
            cancels.get(i).visible = state != 0;
            cancels.get(i).active = state != 0;
            if (state == 0) {
                continue;
            }
            any = true;
            ItemStack stack = menu.slots.get(i).getItem();
            int y = topPos + 30 + i * 20;
            Ui.text(g, font, Component.translatable("gui.barelystorage.craft_amount", stack.getCount()).copy().append(" ").append(stack.getHoverName()), leftPos + 30, y, Ui.TEXT);
            int done = menu.data(i * QueueMenu.PER_JOB + 1);
            int total = Math.max(1, menu.data(i * QueueMenu.PER_JOB + 2));
            Ui.bar(g, leftPos + 30, y + 11, 90, 5, (double) done / total, state == 3 ? Ui.RED : state == 1 ? Ui.GREEN : Ui.BLUE);
            String key = state == 1 ? "gui.barelystorage.queue_running" : state == 2 ? "gui.barelystorage.queue_waiting" : "gui.barelystorage.queue_halted";
            Ui.text(g, font, Component.translatable(key), leftPos + 124, y + 9, state == 3 ? Ui.RED : Ui.MUTED);
        }
        if (!any) {
            Ui.text(g, font, Component.translatable(menu.data(QueueMenu.D_ONLINE) == 0 ? "gui.barelystorage.offline" : "gui.barelystorage.queue_empty"), leftPos + 30, topPos + 38, Ui.MUTED);
        }
        int err = menu.data(QueueMenu.D_ERROR);
        if (err != 0) {
            Component msg = switch (err) {
                case 1 -> Component.translatable("gui.barelystorage.error_no_core");
                case 2 -> Component.translatable("gui.barelystorage.error_depth_short", menu.data(QueueMenu.D_NEED_A), menu.data(QueueMenu.D_HAVE_A));
                case 3 -> Component.translatable("gui.barelystorage.error_ram", menu.data(QueueMenu.D_NEED_A), menu.data(QueueMenu.D_HAVE_A));
                default -> Component.translatable("gui.barelystorage.error_other");
            };
            int y = topPos + menu.playerY() - 24;
            g.fill(leftPos + 5, y, leftPos + 171, y + 12, 0xFF3A1414);
            g.fill(leftPos + 5, y, leftPos + 7, y + 12, Ui.RED);
            Ui.text(g, font, msg, leftPos + 10, y + 2, Ui.RED);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        Ui.text(g, font, title, titleLabelX, titleLabelY, Ui.title());
        Ui.text(g, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Ui.MUTED);
    }
}
