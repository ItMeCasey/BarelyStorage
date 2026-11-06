package com.itmecasey.barelystorage.neoforge.dev;

import com.itmecasey.barelystorage.compat.StorageJeiPlugin;
import java.util.List;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs what JEI does when the plus button of the Bread recipe is clicked in the open terminal. Only built with -PdevTools. */
public final class JeiProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyStorage");

    private JeiProbe() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void run(net.minecraft.client.gui.screens.Screen screen) {
        var runtime = StorageJeiPlugin.runtime;
        if (runtime == null || !(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container)) {
            LOGGER.error("BARELYSTORAGE-SHOTS jei: runtime={} screen={}", runtime, screen);
            return;
        }
        var focuses = runtime.getJeiHelpers().getFocusFactory();
        var focus = focuses.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(Items.BREAD));
        var recipe = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(focus)).get().findFirst().orElse(null);
        var category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
        if (recipe == null) {
            LOGGER.error("BARELYSTORAGE-SHOTS jei: no bread recipe");
            return;
        }
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe, focuses.createFocusGroup(List.of(focus))).orElse(null);
        var menu = container.getMenu();
        LOGGER.info("BARELYSTORAGE-SHOTS jei: menu class={} type={}", menu.getClass().getName(), net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()));
        for (var kind : com.itmecasey.barelystorage.menu.TerminalMenu.Kind.values()) {
            for (int rows : com.itmecasey.barelystorage.menu.TerminalMenu.ROWS) {
                if (kind != com.itmecasey.barelystorage.menu.TerminalMenu.Kind.STORAGE && com.itmecasey.barelystorage.ModContent.terminalMenu(kind, rows) == menu.getType()) {
                    LOGGER.info("BARELYSTORAGE-SHOTS jei: menu type is terminalMenu({}, {})", kind, rows);
                }
            }
        }
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(container.getMenu(), category).orElse(null);
        LOGGER.info("BARELYSTORAGE-SHOTS jei: layout={} handler={}", layout != null, handler != null);
        if (layout == null || handler == null) {
            return;
        }
        var check = ((mezz.jei.api.recipe.transfer.IRecipeTransferHandler) handler).transferRecipe(container.getMenu(), recipe, layout.getRecipeSlotsView(),
                Minecraft.getInstance().player, false, false);
        LOGGER.info("BARELYSTORAGE-SHOTS jei: check (no transfer) answered {}", check == null ? "ok" : check.getClass().getSimpleName());
        var done = ((mezz.jei.api.recipe.transfer.IRecipeTransferHandler) handler).transferRecipe(container.getMenu(), recipe, layout.getRecipeSlotsView(),
                Minecraft.getInstance().player, false, true);
        LOGGER.info("BARELYSTORAGE-SHOTS jei: transfer answered {}", done == null ? "ok" : done.getClass().getSimpleName());
    }
}
