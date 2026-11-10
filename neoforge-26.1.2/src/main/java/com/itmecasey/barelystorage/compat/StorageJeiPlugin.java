package com.itmecasey.barelystorage.compat;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.client.ViewerFill;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/** JEI: the plus button of a crafting recipe fills the grid of a Crafting or Pattern Terminal. Loaded only when JEI is installed. */
@JeiPlugin
public class StorageJeiPlugin implements IModPlugin {
    /** JEI's runtime while a world is open; the development tour uses it to run a plus click. */
    public static volatile mezz.jei.api.runtime.IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        // JEI asks for this while mods are still loading; touching ModContent here would build the blocks too early.
        return Identifier.fromNamespaceAndPath("barelystorage", "jei");
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        StorageJeiPlugin.runtime = runtime;
        com.itmecasey.barelystorage.client.SearchSync.register(com.itmecasey.barelystorage.client.SearchSync.JEI, () -> runtime.getIngredientFilter().getFilterText(),
                text -> runtime.getIngredientFilter().setFilterText(text));
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        com.itmecasey.barelystorage.client.SearchSync.register(com.itmecasey.barelystorage.client.SearchSync.JEI, null, null);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // One handler for every terminal menu: JEI keeps one handler per menu class and recipe type, so six handlers (one per menu
        // type) would replace each other and only the last would ever answer.
        registration.addRecipeTransferHandler(new Handler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    private record Handler(IRecipeTransferHandlerHelper helper) implements IRecipeTransferHandler<TerminalMenu, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends TerminalMenu> getContainerClass() {
            return TerminalMenu.class;
        }

        @Override
        public Optional<MenuType<TerminalMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Nullable
        @Override
        public IRecipeTransferError transferRecipe(TerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
            if (!ViewerFill.accepts(menu)) {
                return helper.createInternalError();
            }
            if (!doTransfer) {
                return null;
            }
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            Map<Integer, ?> positions = helper.getGuiSlotIndexToIngredientMap(recipe);
            List<Integer> keys = new ArrayList<>(positions.keySet());
            java.util.Collections.sort(keys);
            ItemStack[] grid = new ItemStack[TerminalMenu.GRID];
            for (int i = 0; i < inputs.size(); i++) {
                // Crafting grid slots in a vanilla crafting menu are numbered from 1; slot 0 is the result.
                int cell = i < keys.size() ? keys.get(i) - 1 : i;
                if (cell >= 0 && cell < TerminalMenu.GRID) {
                    grid[cell] = ViewerFill.pick(inputs.get(i).getItemStacks().toList(), menu, player);
                }
            }
            ViewerFill.send(menu, grid);
            return null;
        }
    }
}
