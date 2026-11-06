package com.itmecasey.barelystorage.compat;

import com.itmecasey.barelystorage.client.ViewerFill;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.IRecipeTransferHandler;
import com.itmecasey.bei.api.ITransferRegistration;
import com.itmecasey.bei.api.TransferContext;
import com.itmecasey.bei.api.TransferState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** BEI: the plus button of a crafting recipe fills the grid of a Crafting or Pattern Terminal. Loaded only when BEI is installed. */
@BeiPlugin
public class StorageBeiPlugin implements IBeiPlugin {
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
