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
 * The Storage Terminal. The visible rows of the network's items are a read-only container the server refills and Minecraft syncs
 * like any inventory (counts may exceed a stack), so no custom packets are needed. Scrolling, sort, view, search and craft
 * requests arrive as menu button ids.
 */
public class TerminalMenu extends StorageMenu {
    public static final int COLS = 9;
    public static final int LIST_X = 8;
    public static final int LIST_Y = 34;
    public static final int[] ROWS = {5, 7, 9};
    // Menu button ids.
    public static final int BTN_CLEAR = 4;
    public static final int BTN_BACKSPACE = 5;
    public static final int BTN_CHAR = 100000;
    public static final int BTN_SCROLL = 200000;
    public static final int BTN_SORT = 300000;
    public static final int BTN_ORDER = 300010;
    public static final int BTN_VIEW = 300020;
    public static final int BTN_HEIGHT = 300030;
    public static final int BTN_CRAFT = 1000000;
    // Sort methods, orders and views.
    public static final int SORT_QUANTITY = 0;
    public static final int SORT_MOD = 1;
    public static final int SORT_NAME = 2;
    public static final int VIEW_BOTH = 0;
    public static final int VIEW_AVAILABLE = 1;
    public static final int VIEW_CRAFTABLE = 2;

    private static final Map<ItemKey, String> NAMES = new HashMap<>();

    private record Entry(ItemKey key, long count, boolean craftable, String name) {
    }

    private final int rows;
    private final int view;
    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Container list;
    private final ContainerData data;
    private final ItemKey[] slotKeys;
    private String query = "";
    private int scroll;
    private int sort;
    private int order;
    private int viewMode;
    private long lastRefresh = -1000;
    private boolean dirty = true;
    public final int playerY;

    /** Client side. */
    public TerminalMenu(int rows, int id, Inventory inventory) {
        this(rows, id, inventory, null, null);
    }

    /** Server side. */
    public TerminalMenu(int rows, int id, Inventory inventory, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.terminalMenu(rows), id);
        this.rows = rows;
        this.view = rows * COLS;
        this.level = level;
        this.pos = pos;
        this.list = new SimpleContainer(view);
        this.data = new SimpleContainerData(view + 8);
        this.slotKeys = new ItemKey[view];
        for (int row = 0; row < rows; row++) {
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
        this.playerY = LIST_Y + rows * 18 + 16;
        playerSlots(inventory, 8, playerY);
        addDataSlots(data);
    }

    public int rows() {
        return rows;
    }

    public int viewSlots() {
        return view;
    }

    // Data indexes after the per-slot craftable flags.
    public int dScroll() {
        return view;
    }

    public int dMaxScroll() {
        return view + 1;
    }

    public int dOnline() {
        return view + 2;
    }

    public int data(int index) {
        return data.get(index);
    }

    public boolean craftableAt(int slot) {
        return slot >= 0 && slot < view && data.get(slot) != 0;
    }

    public int scroll() {
        return data.get(dScroll());
    }

    public int maxScroll() {
        return data.get(dMaxScroll());
    }

    public boolean online() {
        return data.get(dOnline()) != 0;
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
            Map<ItemKey, Long> stored = net.pool().view();
            Set<ItemKey> craftable = new HashSet<>();
            for (Pattern p : net.patterns()) {
                for (Pattern.Stack out : p.outputs()) {
                    craftable.add(out.key());
                }
            }
            String q = query.toLowerCase(Locale.ROOT);
            Set<ItemKey> shown = new HashSet<>();
            if (viewMode != VIEW_CRAFTABLE) {
                for (Map.Entry<ItemKey, Long> e : stored.entrySet()) {
                    shown.add(e.getKey());
                    if (matches(e.getKey(), q, net)) {
                        entries.add(new Entry(e.getKey(), e.getValue(), false, nameOf(e.getKey(), net)));
                    }
                }
            }
            if (viewMode != VIEW_AVAILABLE) {
                for (ItemKey key : craftable) {
                    if (viewMode == VIEW_BOTH && !shown.add(key)) {
                        continue;
                    }
                    if (matches(key, q, net)) {
                        long have = stored.getOrDefault(key, 0L);
                        entries.add(new Entry(key, have, have == 0, nameOf(key, net)));
                    }
                }
            }
        }
        entries.sort(comparator());
        int maxScroll = Math.max(0, (entries.size() + COLS - 1) / COLS - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < view; i++) {
            int at = scroll * COLS + i;
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
        data.set(dScroll(), scroll);
        data.set(dMaxScroll(), maxScroll);
        data.set(dOnline(), online ? 1 : 0);
    }

    private Comparator<Entry> comparator() {
        Comparator<Entry> byName = Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::key);
        Comparator<Entry> c = switch (sort) {
            case SORT_QUANTITY -> Comparator.comparingLong(Entry::count).thenComparing(byName);
            case SORT_MOD -> Comparator.<Entry, String>comparing(e -> namespace(e.key())).thenComparing(byName);
            default -> byName;
        };
        return order == 1 ? c.reversed() : c;
    }

    private static String namespace(ItemKey key) {
        int colon = key.id().indexOf(':');
        return colon < 0 ? "minecraft" : key.id().substring(0, colon);
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
        if (id >= BTN_CRAFT) {
            int code = id - BTN_CRAFT;
            int slot = code / 10000;
            int amount = Math.max(1, code % 10000);
            if (slot < view && slotKeys[slot] != null && player instanceof ServerPlayer sp && pos != null) {
                request(StorageNetwork.find(level, pos), slotKeys[slot], amount, sp);
            }
            return true;
        }
        if (id >= BTN_HEIGHT && id < BTN_HEIGHT + 3) {
            if (player instanceof ServerPlayer sp && pos != null) {
                TerminalMenus.setHeight(sp, id - BTN_HEIGHT);
                TerminalMenus.open(sp, level, pos);
            }
            return true;
        }
        if (id >= BTN_VIEW && id < BTN_VIEW + 3) {
            viewMode = id - BTN_VIEW;
        } else if (id >= BTN_ORDER && id < BTN_ORDER + 2) {
            order = id - BTN_ORDER;
        } else if (id >= BTN_SORT && id < BTN_SORT + 3) {
            sort = id - BTN_SORT;
        } else if (id >= BTN_SCROLL && id < BTN_SCROLL + 100000) {
            scroll = id - BTN_SCROLL;
        } else if (id >= BTN_CHAR && id < BTN_CHAR + 65536) {
            int c = id - BTN_CHAR;
            if (c >= 32 && query.length() < 40) {
                query += (char) c;
                scroll = 0;
            }
        } else if (id == BTN_CLEAR) {
            query = "";
            scroll = 0;
        } else if (id == BTN_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scroll = 0;
            }
        } else {
            return false;
        }
        dirty = true;
        return true;
    }

    // ---- Server: clicks on the item list ----

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < view) {
            if (level != null && !level.isClientSide() && pos != null && player instanceof ServerPlayer sp) {
                listClick(slotIndex, button, input, sp);
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    /** Taking out: a click takes one item, shift-click a stack. Putting in follows vanilla: left drops what you hold, right drops one. */
    private void listClick(int slot, int button, ContainerInput input, ServerPlayer player) {
        StorageNetwork net = StorageNetwork.find(level, pos);
        if (!net.online()) {
            return;
        }
        ItemKey key = slotKeys[slot];
        ItemStack carried = getCarried();
        if (input == ContainerInput.QUICK_MOVE) {
            if (key != null && !craftableAt(slot)) {
                ItemStack out = net.extract(key, Keys.maxStack(key));
                if (!out.isEmpty()) {
                    moveItemStackTo(out, view, slots.size(), true);
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
            } else if (key != null && !craftableAt(slot) && button == 0) {
                setCarried(net.extract(key, 1));
            }
        }
        dirty = true;
    }

    private void request(StorageNetwork net, ItemKey key, int amount, ServerPlayer player) {
        CraftPlan plan = net.requestCraft(key, amount);
        if (!plan.ok()) {
            player.sendSystemMessage(Component.translatable("message.barelystorage.craft_failed", plan.message));
        } else {
            player.sendSystemMessage(Component.translatable("message.barelystorage.craft_started", amount,
                    Keys.stack(key, 1, net.registries()).getHoverName(), plan.steps.size()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < view || level == null || level.isClientSide() || pos == null) {
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
