package com.itmecasey.barelystorage.neoforge.dev;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs what REI does when the plus button of the Bread recipe is clicked in the open terminal. Only built with -PdevTools. */
public final class ReiProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyStorage");

    private ReiProbe() {
    }

    public static void run(net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
            LOGGER.error("BARELYSTORAGE-SHOTS rei: screen={}", screen);
            return;
        }
        Display bread = null;
        for (Display d : DisplayRegistry.getInstance().get(BuiltinPlugin.CRAFTING)) {
            for (var out : d.getOutputEntries()) {
                for (EntryStack<?> e : out) {
                    if (e.getValue() instanceof ItemStack s && s.is(Items.BREAD)) {
                        bread = d;
                    }
                }
            }
        }
        LOGGER.info("BARELYSTORAGE-SHOTS rei: bread display found={}", bread != null);
        if (bread == null) {
            return;
        }
        var context = TransferHandler.Context.create(true, false, container, bread);
        int tried = 0;
        for (TransferHandler handler : TransferHandlerRegistry.getInstance()) {
            tried++;
            TransferHandler.Result result = handler.handle(context);
            if (result.isApplicable()) {
                LOGGER.info("BARELYSTORAGE-SHOTS rei: handler {} answered successful={} blocking={}", handler.getClass().getName(), result.isSuccessful(), result.isBlocking());
                if (result.isBlocking() || result.isSuccessful()) {
                    return;
                }
            }
        }
        LOGGER.error("BARELYSTORAGE-SHOTS rei: none of {} handlers applied", tried);
    }
}
