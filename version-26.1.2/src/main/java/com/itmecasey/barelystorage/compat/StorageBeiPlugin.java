package com.itmecasey.barelystorage.compat;

import com.itmecasey.barelystorage.client.ViewerFill;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.IRecipeTransferHandler;
import com.itmecasey.bei.api.ISearchAccess;
import com.itmecasey.bei.api.IStorageRegistration;
import com.itmecasey.bei.api.ITransferRegistration;
import com.itmecasey.bei.api.TransferContext;
import com.itmecasey.bei.api.TransferState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** BEI: the plus button of a crafting recipe fills the grid of a Crafting or Pattern Terminal. Loaded only when BEI is installed. */
@BeiPlugin
public class StorageBeiPlugin implements IBeiPlugin {
    /**
     * The Master Blueprint counts what the open terminal's list shows: the client only has the rows on screen (the scrolled
     * window of the list), so with no terminal open, or for items scrolled out of view, nothing is reported.
     */
    @Override
    public void registerStorageSources(IStorageRegistration registration) {
        registration.register("barelystorage:network", (player, counter) -> {
            var screen = com.itmecasey.barelystorage.client.TerminalScreen.current();
            if (screen != null) {
                screen.countVisible(counter::add);
            }
        });
    }

    @Override
    public void registerSearch(ISearchAccess search) {
        com.itmecasey.barelystorage.client.SearchSync.register(com.itmecasey.barelystorage.client.SearchSync.BEI, search::getText, search::setText);
    }

    @Override
    public void registerTransfers(ITransferRegistration registration) {
        registration.register(new IRecipeTransferHandler<TerminalMenu>() {
            @Override
            public Class<TerminalMenu> menuClass() {
                return TerminalMenu.class;
            }

            @Override
            public TransferState check(TransferContext<TerminalMenu> ctx) {
                String type = ctx.recipe().type();
                if (!ViewerFill.accepts(ctx.menu()) || !(type.equals("ShapedCrafting") || type.equals("ShapelessCrafting"))) {
                    return TransferState.hidden();
                }
                return TransferState.enabled();
            }

            @Override
            public boolean transfer(TransferContext<TerminalMenu> ctx) {
                TerminalMenu menu = ctx.menu();
                var recipe = ctx.recipe();
                var player = Minecraft.getInstance().player;
                if (player == null || !ViewerFill.accepts(menu)) {
                    return false;
                }
                int width = Math.max(0, recipe.width());
                ItemStack[] cells = new ItemStack[TerminalMenu.GRID];
                for (int i = 0; i < recipe.inputs().size(); i++) {
                    int cell = width > 0 ? (i / width) * 3 + (i % width) : i;
                    if (cell >= 0 && cell < TerminalMenu.GRID) {
                        cells[cell] = ViewerFill.pick(recipe.inputs().get(i), menu, player);
                    }
                }
                ViewerFill.send(menu, cells);
                return true;
            }
        });
    }
}
