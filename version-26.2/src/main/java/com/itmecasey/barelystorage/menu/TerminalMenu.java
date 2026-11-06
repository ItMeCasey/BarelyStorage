package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.CraftPlan;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.StorageNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The Storage Terminal. The visible page of the network's items is a read-only container the server refills and Minecraft
 * syncs like any inventory (counts may exceed a stack), so no custom packets are needed. Paging, sorting, search and the
 * crafting amount arrive as menu button ids.
 */
public class TerminalMenu extends StorageMenu {
    public static final int COLS = 9;
    public static final int ROWS = 5;
    public static final int VIEW = COLS * ROWS;
    public static final int LIST_X = 8;
    public static final int LIST_Y = 36;
    // Data indexes after the VIEW craftable flags.
    public static final int D_PAGE = VIEW;
    public static final int D_SORT = VIEW + 1;
    public static final int D_AMOUNT = VIEW + 2;
    public static final int D_ONLINE = VIEW + 3;
    public static final int D_MAXPAGE = VIEW + 4;
    public static final int D_COUNT = VIEW + 5;
    public static final int BTN_PREV = 0;
    public static final int BTN_NEXT = 1;
    public static final int BTN_SORT = 2;
    public static final int BTN_AMOUNT = 3;
    public static final int BTN_CLEAR = 4;
    public static final int BTN_BACKSPACE = 5;
    public static final int BTN_CHAR = 100000;
    public static final int[] AMOUNTS = {1, 8, 64};

    private static final Map<ItemKey, String> NAMES = new HashMap<>();

    private record Entry(ItemKey key, long count, boolean craftable, String name) {
    }

    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Container list = new SimpleContainer(VIEW);
    private final ContainerData data = new SimpleContainerData(D_COUNT);
    private final ItemKey[] slotKeys = new ItemKey[VIEW];
    private String query = "";
    private int page;
    private int sort;
    private int amount;
    private long lastRefresh = -1000;
    private boolean dirty = true;

    /** Client side. */
    public TerminalMenu(int id, Inventory inventory) {
        this(id, inventory, null, null);
    }

    /** Server side. */
    public TerminalMenu(int id, Inventory inventory, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.TERMINAL_MENU, id);
        this.level = level;
        this.pos = pos;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new Slot(list, col + row * COLS, LIST_X + col * 18, LIST_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }
        playerSlots(inventory, 8, 148);
        addDataSlots(data);
    }

    public int data(int index) {
        return data.get(index);
    }

    public boolean craftableAt(int slot) {
        return data.get(slot) != 0;
    }

    // ---- Server: listing ----

    private void refresh() {
        if (level == null || pos == null || level.isClientSide()) {
            return;
        }
        dirty = false;
        lastRefresh = level.getGameTime();
        StorageNetwork net = StorageNetwork.find(level, pos);
        List<Entry> entries = new ArrayList<>();
        boolean online = net.online();
        if (online) {
            Map<ItemKey, Long> view = net.pool().view();
            Set<ItemKey> have = new HashSet<>();
            String q = query.toLowerCase(Locale.ROOT);
            for (Map.Entry<ItemKey, Long> e : view.entrySet()) {
                have.add(e.getKey());
                if (matches(e.getKey(), q, net)) {
                    entries.add(new Entry(e.getKey(), e.getValue(), false, nameOf(e.getKey(), net)));
                }
            }
            for (Pattern p : net.patterns()) {
                for (Pattern.Stack out : p.outputs()) {
                    if (have.add(out.key()) && matches(out.key(), q, net)) {
                        entries.add(new Entry(out.key(), 0, true, nameOf(out.key(), net)));
                    }
                }
            }
        }
        Comparator<Entry> byName = Comparator.comparing(Entry::name).thenComparing(Entry::key);
        entries.sort(sort == 1 ? Comparator.comparingLong(Entry::count).reversed().thenComparing(byName) : byName);
        int maxPage = Math.max(0, (entries.size() - 1) / VIEW);
        page = Math.max(0, Math.min(page, maxPage));
        for (int i = 0; i < VIEW; i++) {
            int at = page * VIEW + i;
            if (at < entries.size()) {
                Entry e = entries.get(at);
                slotKeys[i] = e.key();
                list.setItem(i, Keys.stack(e.key(), (int) Math.max(1, Math.min(Integer.MAX_VALUE, e.count())), net.registries()));
                data.set(i, e.craftable() ? 1 : 0);
            } else {
                slotKeys[i] = null;
                list.setItem(i, ItemStack.EMPTY);
                data.set(i, 0);
            }
        }
        data.set(D_PAGE, page);
        data.set(D_SORT, sort);
        data.set(D_AMOUNT, amount);
        data.set(D_ONLINE, online ? 1 : 0);
        data.set(D_MAXPAGE, maxPage);
    }

    private static boolean matches(ItemKey key, String q, StorageNetwork net) {
        return q.isEmpty() || key.id().contains(q) || nameOf(key, net).toLowerCase(Locale.ROOT).contains(q);
    }

    private static String nameOf(ItemKey key, StorageNetwork net) {
        return NAMES.computeIfAbsent(key, k -> Keys.stack(k, 1, net.registries()).getHoverName().getString());
    }

    @Override
    public void broadcastChanges() {
        if (level != null && !level.isClientSide() && (dirty || level.getGameTime() - lastRefresh >= 10)) {
            refresh();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide()) {
            return true;
        }
        if (id >= BTN_CHAR) {
            int c = id - BTN_CHAR;
            if (c >= 32 && query.length() < 40) {
                query += (char) c;
                page = 0;
            }
        } else {
            switch (id) {
                case BTN_PREV -> page = Math.max(0, page - 1);
                case BTN_NEXT -> page++;
                case BTN_SORT -> sort = 1 - sort;
                case BTN_AMOUNT -> amount = (amount + 1) % AMOUNTS.length;
                case BTN_CLEAR -> {
                    query = "";
                    page = 0;
                }
                case BTN_BACKSPACE -> {
                    if (!query.isEmpty()) {
                        query = query.substring(0, query.length() - 1);
                        page = 0;
                    }
                }
                default -> {
                    return false;
                }
            }
        }
        dirty = true;
        return true;
    }

    // ---- Server: clicks on the item list ----

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < VIEW) {
            if (level != null && !level.isClientSide() && pos != null && player instanceof ServerPlayer sp) {
                listClick(slotIndex, button, input, sp);
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void listClick(int slot, int button, ContainerInput input, ServerPlayer player) {
        StorageNetwork net = StorageNetwork.find(level, pos);
        if (!net.online()) {
            return;
        }
        ItemKey key = slotKeys[slot];
        ItemStack carried = getCarried();
        if (input == ContainerInput.CLONE) {
            if (key != null) {
                request(net, key, player);
            }
        } else if (input == ContainerInput.QUICK_MOVE) {
            if (key != null) {
                ItemStack out = net.extract(key, Keys.maxStack(key));
                if (!out.isEmpty()) {
                    moveItemStackTo(out, VIEW, slots.size(), true);
                    if (!out.isEmpty()) {
                        net.insert(out);
                    }
                }
            }
        } else if (input == ContainerInput.PICKUP) {
            if (!carried.isEmpty()) {
                ItemStack offer = button == 1 ? carried.copyWithCount(1) : carried.copy();
                ItemStack rest = net.insert(offer);
                int moved = offer.getCount() - rest.getCount();
                carried.shrink(moved);
                setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            } else if (key != null && !craftableAt(slot)) {
                int max = Keys.maxStack(key);
                ItemStack out = net.extract(key, button == 1 ? Math.max(1, max / 2) : max);
                setCarried(out);
            }
        }
        dirty = true;
    }

    private void request(StorageNetwork net, ItemKey key, ServerPlayer player) {
        CraftPlan plan = net.requestCraft(key, AMOUNTS[amount]);
        if (!plan.ok()) {
            player.sendSystemMessage(Component.translatable("message.barelystorage.craft_failed", plan.message));
        } else {
            player.sendSystemMessage(Component.translatable("message.barelystorage.craft_started", AMOUNTS[amount],
                    Keys.stack(key, 1, net.registries()).getHoverName(), plan.steps.size()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < VIEW || level == null || level.isClientSide() || pos == null) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        StorageNetwork net = StorageNetwork.find(level, pos);
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        ItemStack rest = net.insert(stack.copy());
        if (rest.getCount() == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(rest);
        dirty = true;
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
