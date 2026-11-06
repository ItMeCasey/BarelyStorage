package com.itmecasey.barelystorage.compat;

import com.itmecasey.barelystorage.client.ViewerFill;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.ItemStack;

/** REI: the plus button of a crafting recipe fills the grid of a Crafting or Pattern Terminal. Loaded only when REI is installed. */
@REIPluginClient
public class StorageReiPlugin implements REIClientPlugin {
    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(context -> {
            if (!(context.getMenu() instanceof TerminalMenu menu) || !ViewerFill.accepts(menu)) {
                return TransferHandler.Result.createNotApplicable();
            }
            Display display = context.getDisplay();
            if (!(display instanceof SimpleGridMenuDisplay grid)) {
                return TransferHandler.Result.createNotApplicable();
            }
            if (context.isActuallyCrafting()) {
                List<EntryIngredient> inputs = display.getInputEntries();
                int width = Math.max(1, grid.getWidth());
                ItemStack[] cells = new ItemStack[TerminalMenu.GRID];
                for (int i = 0; i < inputs.size(); i++) {
                    int cell = (i / width) * 3 + (i % width);
                    if (cell < TerminalMenu.GRID) {
                        List<ItemStack> candidates = new ArrayList<>();
                        for (EntryStack<?> entry : inputs.get(i)) {
                            if (entry.getValue() instanceof ItemStack stack) {
                                candidates.add(stack);
                            }
                        }
                        cells[cell] = ViewerFill.pick(candidates, menu, context.getMinecraft().player);
                    }
                }
                ViewerFill.send(menu, cells);
            }
            return TransferHandler.Result.createSuccessful().blocksFurtherHandling();
        });
    }
}
