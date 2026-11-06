package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.item.PatternItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The Pattern Terminal: lay a recipe out on the grid and encode it onto a blank Circuit Pattern. */
public class PatternMenu extends StorageMenu {
    public static final int GRID = 9;
    public static final int OUTS = 3;
    public static final int S_GRID = 0;
    public static final int S_OUT = GRID;
    public static final int S_RESULT = GRID + OUTS;
    public static final int S_BLANK = S_RESULT + 1;
    public static final int S_ENCODED = S_BLANK + 1;
    public static final int FIRST_PLAYER = S_ENCODED + 1;
    public static final int BTN_MODE = 0;
    public static final int BTN_ENCODE = 1;
    public static final int BTN_CLEAR = 2;

    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Container ghost = new SimpleContainer(GRID + OUTS);
    private final Container result = new SimpleContainer(1);
    private final Container blank = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof PatternItem p && p.type() == PatternItem.Type.BLANK;
        }
    };
    private final Container encoded = new SimpleContainer(1);
    private final ContainerData data = new SimpleContainerData(1);

    /** Client side. */
    public PatternMenu(int id, Inventory inventory) {
        this(id, inventory, null, null);
    }

    public PatternMenu(int id, Inventory inventory, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.PATTERN_MENU, id);
        this.level = level;
        this.pos = pos;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new GhostSlot(ghost, col + row * 3, 30 + col * 18, 24 + row * 18));
            }
        }
        for (int i = 0; i < OUTS; i++) {
            addSlot(new GhostSlot(ghost, GRID + i, 96, 24 + i * 18));
        }
        addSlot(new Slot(result, 0, 140, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        addSlot(new Slot(blank, 0, 30, 90) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(0, stack);
            }
        });
        addSlot(new Slot(encoded, 0, 84, 90) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        playerSlots(inventory, 8, 134);
        addDataSlots(data);
    }

    /** A slot that shows a copy of an item and never holds the real one. */
    private static final class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    public boolean processing() {
        return data.get(0) == 1;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < GRID + OUTS) {
            ghostClick(slotIndex, button);
            return;
        }
        if (slotIndex == S_RESULT) {
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void ghostClick(int slot, int button) {
        ItemStack carried = getCarried();
        ItemStack here = ghost.getItem(slot);
        if (carried.isEmpty()) {
            if (button == 1 && !here.isEmpty() && here.getCount() > 1) {
                here.shrink(1);
            } else {
                ghost.setItem(slot, ItemStack.EMPTY);
            }
        } else if (button == 1 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, carried)) {
            here.setCount(Math.min(here.getCount() + 1, 64));
        } else {
            ghost.setItem(slot, carried.copyWithCount(1));
        }
        updateResult();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide()) {
            return true;
        }
        switch (id) {
            case BTN_MODE -> {
                data.set(0, 1 - data.get(0));
                updateResult();
            }
            case BTN_CLEAR -> {
                for (int i = 0; i < GRID + OUTS; i++) {
                    ghost.setItem(i, ItemStack.EMPTY);
                }
                updateResult();
            }
            case BTN_ENCODE -> encode();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void updateResult() {
        ItemStack out = ItemStack.EMPTY;
        if (level instanceof ServerLevel server && !processing()) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < GRID; i++) {
                items.add(ghost.getItem(i).copy());
            }
            CraftingInput input = CraftingInput.of(3, 3, items);
            Optional<RecipeHolder<CraftingRecipe>> recipe = server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
            if (recipe.isPresent()) {
                out = recipe.get().value().assemble(input);
            }
        }
        result.setItem(0, out);
    }

    private void encode() {
        if (level == null) {
            return;
        }
        ItemStack blankStack = blank.getItem(0);
        if (blankStack.isEmpty() || !encoded.getItem(0).isEmpty()) {
            return;
        }
        var reg = level.registryAccess();
        Map<com.itmecasey.barelystorage.ItemKey, Integer> in = new LinkedHashMap<>();
        for (int i = 0; i < GRID; i++) {
            ItemStack s = ghost.getItem(i);
            if (!s.isEmpty()) {
                in.merge(Keys.of(s, reg), s.getCount(), Integer::sum);
            }
        }
        List<Pattern.Stack> inputs = new ArrayList<>();
        in.forEach((k, n) -> inputs.add(new Pattern.Stack(k, n)));
        List<Pattern.Stack> outputs = new ArrayList<>();
        if (processing()) {
            for (int i = 0; i < OUTS; i++) {
                ItemStack s = ghost.getItem(GRID + i);
                if (!s.isEmpty()) {
                    outputs.add(new Pattern.Stack(Keys.of(s, reg), s.getCount()));
                }
            }
        } else {
            ItemStack r = result.getItem(0);
            if (!r.isEmpty()) {
                outputs.add(new Pattern.Stack(Keys.of(r, reg), r.getCount()));
            }
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return;
        }
        ItemStack out = new ItemStack(processing() ? ModContent.item("processing_pattern") : ModContent.item("crafting_pattern"));
        PatternItem.write(out, new Pattern(processing() ? Pattern.Kind.PROCESSING : Pattern.Kind.CRAFTING, inputs, outputs));
        blankStack.shrink(1);
        blank.setChanged();
        encoded.setItem(0, out);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < FIRST_PLAYER && index >= S_BLANK) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, FIRST_PLAYER, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.setChanged();
            return copy;
        }
        if (index >= FIRST_PLAYER) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.getItem() instanceof PatternItem p && p.type() == PatternItem.Type.BLANK) {
                ItemStack copy = stack.copy();
                if (moveItemStackTo(stack, S_BLANK, S_BLANK + 1, false)) {
                    slot.setChanged();
                    return copy;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (level != null && !level.isClientSide()) {
            clearContainer(player, blank);
            clearContainer(player, encoded);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
