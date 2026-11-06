package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.CraftPlan;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.item.PatternItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The terminals that show the network's items: Storage Terminal, Crafting Terminal (adds a 3x3 grid that crafts from the network and
 * your inventory) and Pattern Terminal (adds the grid, outputs and blank pattern slots for encoding). The visible rows of items are
 * a read-only container the server refills and Minecraft syncs like any inventory (counts may exceed a stack), so no custom packets
 * are needed. Scrolling, sort, view, search, craft requests and grid fills arrive as menu button ids.
 */
public class TerminalMenu extends StorageMenu {
    public enum Kind { STORAGE, CRAFTING, PATTERN }

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
    public static final int BTN_MODE = 400000;
    public static final int BTN_TAGS = 400001;
    public static final int BTN_ENCODE = 400002;
    public static final int BTN_GRID_CLEAR = 400003;
    /** Fills a grid slot: BTN_SET + slot * 10000 + item registry id (0 clears it). */
    public static final int BTN_SET = 5000000;
    public static final int BTN_CRAFT = 1000000;
    // Sort methods and views.
    public static final int SORT_QUANTITY = 0;
    public static final int SORT_MOD = 1;
    public static final int SORT_NAME = 2;
    public static final int VIEW_BOTH = 0;
    public static final int VIEW_AVAILABLE = 1;
    public static final int VIEW_CRAFTABLE = 2;
    public static final int GRID = 9;
    public static final int OUTS = 3;

    private static final Map<ItemKey, String> NAMES = new HashMap<>();

    private record Entry(ItemKey key, long count, boolean craftable, String name) {
    }

    private final Kind kind;
    private final int rows;
    private final int view;
    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Container list;
    private final ContainerData data;
    private final ItemKey[] slotKeys;
    private final Container grid = new SimpleContainer(GRID);
    private final Container outs = new SimpleContainer(OUTS);
    private final Container result = new SimpleContainer(1);
    private final Container blank = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof PatternItem p && p.type() == PatternItem.Type.BLANK;
        }
    };
    private final Container encoded = new SimpleContainer(1);
    @Nullable
    private RecipeHolder<CraftingRecipe> recipe;
    private String query = "";
    private int scroll;
    private int sort;
    private int order;
    private int viewMode;
    private boolean processing;
    private boolean tags;
    private long lastRefresh = -1000;
    private boolean dirty = true;
    public final int playerY;
    public final int extrasY;
    public final int resultSlot;
    public final int firstPlayerSlot;

    /** Client side. */
    public TerminalMenu(Kind kind, int rows, int id, Inventory inventory) {
        this(kind, rows, id, inventory, null, null);
    }

    /** Server side. */
    public TerminalMenu(Kind kind, int rows, int id, Inventory inventory, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.terminalMenu(kind, rows), id);
        this.kind = kind;
        this.rows = rows;
        this.view = rows * COLS;
        this.level = level;
        this.pos = pos;
        this.list = new SimpleContainer(view);
        this.data = new SimpleContainerData(view + 24);
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
        this.extrasY = LIST_Y + rows * 18 + 8;
        int resultIndex = -1;
        if (kind == Kind.CRAFTING) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    addSlot(new GhostSlot(grid, c + r * 3, 30 + c * 18, extrasY + r * 18));
                }
            }
            resultIndex = slots.size();
            addSlot(new Slot(result, 0, 124, extrasY + 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        } else if (kind == Kind.PATTERN) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    addSlot(new GhostSlot(grid, c + r * 3, 8 + c * 18, extrasY + r * 18));
                }
            }
            for (int i = 0; i < OUTS; i++) {
                addSlot(new GhostSlot(outs, i, 78, extrasY + i * 18));
            }
            resultIndex = slots.size();
            addSlot(new Slot(result, 0, 104, extrasY + 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
            addSlot(new Slot(blank, 0, 142, extrasY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(0, stack);
                }
            });
            addSlot(new Slot(encoded, 0, 142, extrasY + 36) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        this.resultSlot = resultIndex;
        int extraHeight = kind == Kind.STORAGE ? 0 : kind == Kind.CRAFTING ? 58 : 76;
        this.playerY = LIST_Y + rows * 18 + 16 + extraHeight;
        this.firstPlayerSlot = slots.size();
        this.owner = inventory.player;
        playerSlots(inventory, 8, playerY);
        addDataSlots(data);
    }

    public Kind kind() {
        return kind;
    }

    public int rows() {
        return rows;
    }

    public int viewSlots() {
        return view;
    }

    public int dScroll() {
        return view;
    }

    public int dMaxScroll() {
        return view + 1;
    }

    public int dOnline() {
        return view + 2;
    }

    public int dMode() {
        return view + 3;
    }

    public int dTags() {
        return view + 4;
    }

    /** Per grid slot: 1 when its ingredient is not in the network or your inventory. */
    public int dMissing(int gridSlot) {
        return view + 8 + gridSlot;
    }

    /** 0 nothing to craft, 1 ready, 2 blocked by a missing ingredient. */
    public int dResult() {
        return view + 18;
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

    public boolean processing() {
        return data.get(dMode()) != 0;
    }

    public boolean tags() {
        return data.get(dTags()) != 0;
    }

    public boolean gridSlot(int index) {
        return kind != Kind.STORAGE && index >= view && index < view + GRID;
    }

    public boolean outputSlot(int index) {
        return kind == Kind.PATTERN && index >= view + GRID && index < view + GRID + OUTS;
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
        data.set(dMode(), processing ? 1 : 0);
        data.set(dTags(), tags ? 1 : 0);
        if (kind == Kind.CRAFTING) {
            updateCraftingState(net);
        }
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

    // ---- Server: crafting grid ----

    private void updateResult() {
        ItemStack out = ItemStack.EMPTY;
        recipe = null;
        if (level instanceof ServerLevel server && (kind == Kind.CRAFTING || (kind == Kind.PATTERN && !processing))) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < GRID; i++) {
                items.add(grid.getItem(i).copy());
            }
            CraftingInput input = CraftingInput.of(3, 3, items);
            Optional<RecipeHolder<CraftingRecipe>> found = server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
            if (found.isPresent()) {
                recipe = found.get();
                out = recipe.value().assemble(input);
            }
        }
        result.setItem(0, out);
        dirty = true;
    }

    private Map<ItemKey, Integer> gridNeeds(StorageNetwork net) {
        Map<ItemKey, Integer> need = new LinkedHashMap<>();
        for (int i = 0; i < GRID; i++) {
            ItemStack g = grid.getItem(i);
            if (!g.isEmpty()) {
                need.merge(Keys.of(g, net.registries()), g.getCount(), Integer::sum);
            }
        }
        return need;
    }

    private long inventoryCount(Player player, ItemKey key, StorageNetwork net) {
        long n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && Keys.of(s, net.registries()).equals(key)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private Player owner;

    private void updateCraftingState(StorageNetwork net) {
        Map<ItemKey, Integer> need = gridNeeds(net);
        boolean any = !need.isEmpty() && !result.getItem(0).isEmpty();
        boolean blocked = false;
        for (int i = 0; i < GRID; i++) {
            ItemStack g = grid.getItem(i);
            int flag = 0;
            if (!g.isEmpty()) {
                ItemKey key = Keys.of(g, net.registries());
                long have = (net.online() ? net.pool().count(key) : 0) + (owner == null ? 0 : inventoryCount(owner, key, net));
                if (have < need.get(key)) {
                    flag = 1;
                    blocked = true;
                }
            }
            data.set(dMissing(i), flag);
        }
        data.set(dResult(), !any ? 0 : blocked ? 2 : 1);
    }

    private boolean takeIngredients(StorageNetwork net, ServerPlayer player) {
        Map<ItemKey, Integer> need = gridNeeds(net);
        for (Map.Entry<ItemKey, Integer> e : need.entrySet()) {
            long have = (net.online() ? net.pool().count(e.getKey()) : 0) + inventoryCount(player, e.getKey(), net);
            if (have < e.getValue()) {
                return false;
            }
        }
        for (Map.Entry<ItemKey, Integer> e : need.entrySet()) {
            int left = e.getValue();
            while (left > 0 && net.online()) {
                ItemStack got = net.extract(e.getKey(), left);
                if (got.isEmpty()) {
                    break;
                }
                left -= got.getCount();
            }
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && Keys.of(s, net.registries()).equals(e.getKey())) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        return true;
    }

    private void craftClick(ContainerInput input, ServerPlayer player) {
        StorageNetwork net = StorageNetwork.find(level, pos);
        updateResult();
        int crafts = input == ContainerInput.QUICK_MOVE ? 64 : 1;
        for (int n = 0; n < crafts; n++) {
            ItemStack out = result.getItem(0).copy();
            if (out.isEmpty() || recipe == null) {
                break;
            }
            if (input != ContainerInput.QUICK_MOVE) {
                ItemStack carried = getCarried();
                if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, out) || carried.getCount() + out.getCount() > carried.getMaxStackSize())) {
                    break;
                }
            }
            if (!takeIngredients(net, player)) {
                break;
            }
            if (input == ContainerInput.QUICK_MOVE) {
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                    break;
                }
            } else {
                ItemStack carried = getCarried();
                if (carried.isEmpty()) {
                    setCarried(out);
                } else {
                    carried.grow(out.getCount());
                }
            }
        }
        dirty = true;
    }

    private void ghostClick(int slotIndex, int button) {
        Slot slot = slots.get(slotIndex);
        Container container = slot.container;
        int index = slot.getContainerSlot();
        ItemStack carried = getCarried();
        ItemStack here = container.getItem(index);
        if (carried.isEmpty()) {
            if (button == 1 && !here.isEmpty() && here.getCount() > 1) {
                here.shrink(1);
            } else {
                container.setItem(index, ItemStack.EMPTY);
            }
        } else if (button == 1 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, carried)) {
            here.setCount(Math.min(here.getCount() + 1, 64));
        } else {
            container.setItem(index, carried.copyWithCount(1));
        }
        updateResult();
    }

    // ---- Server: pattern encoding ----

    private void encode() {
        if (level == null || kind != Kind.PATTERN) {
            return;
        }
        ItemStack blankStack = blank.getItem(0);
        if (blankStack.isEmpty() || !encoded.getItem(0).isEmpty()) {
            return;
        }
        var reg = level.registryAccess();
        Map<ItemKey, Integer> counts = new LinkedHashMap<>();
        Map<ItemKey, Set<ItemKey>> alts = new LinkedHashMap<>();
        for (int i = 0; i < GRID; i++) {
            ItemStack s = grid.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemKey key = Keys.of(s, reg);
            counts.merge(key, s.getCount(), Integer::sum);
            if (tags) {
                alts.computeIfAbsent(key, k -> new LinkedHashSet<>()).addAll(alternatives(i, s));
            }
        }
        List<Pattern.Stack> inputs = new ArrayList<>();
        counts.forEach((k, n) -> inputs.add(new Pattern.Stack(k, n, new ArrayList<>(alts.getOrDefault(k, Set.of())))));
        List<Pattern.Stack> outputs = new ArrayList<>();
        if (processing) {
            for (int i = 0; i < OUTS; i++) {
                ItemStack s = outs.getItem(i);
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
        ItemStack out = new ItemStack(ModContent.item(processing ? "processing_pattern" : "crafting_pattern"));
        PatternItem.write(out, new Pattern(processing ? Pattern.Kind.PROCESSING : Pattern.Kind.CRAFTING, inputs, outputs));
        blankStack.shrink(1);
        blank.setChanged();
        encoded.setItem(0, out);
    }

    /**
     * Other items that may stand in for the ghost item in one grid slot. Crafting patterns: items that share a tag with it and still
     * make the same recipe. Processing patterns: the members of the smallest tag (2 to 64 items) the item belongs to.
     */
    private Set<ItemKey> alternatives(int gridSlot, ItemStack ghost) {
        Set<ItemKey> out = new LinkedHashSet<>();
        var reg = level.registryAccess();
        Item item = ghost.getItem();
        List<HolderSet.Named<Item>> mine = new ArrayList<>();
        BuiltInRegistries.ITEM.getTags().forEach(named -> {
            if (named.stream().anyMatch(h -> h.value() == item) && named.size() > 1) {
                mine.add(named);
            }
        });
        if (processing) {
            mine.stream().filter(n -> n.size() <= 64).min(Comparator.comparingInt(HolderSet.Named::size)).ifPresent(n -> n.forEach(h -> {
                if (h.value() != item) {
                    out.add(Keys.of(new ItemStack(h.value()), reg));
                }
            }));
            return out;
        }
        if (recipe == null || !(level instanceof ServerLevel server)) {
            return out;
        }
        Set<Item> tried = new HashSet<>();
        for (HolderSet.Named<Item> tag : mine) {
            for (var holder : tag) {
                Item candidate = holder.value();
                if (candidate == item || !tried.add(candidate)) {
                    continue;
                }
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < GRID; i++) {
                    items.add(i == gridSlot ? new ItemStack(candidate) : grid.getItem(i).copy());
                }
                if (recipe.value().matches(CraftingInput.of(3, 3, items), server)) {
                    out.add(Keys.of(new ItemStack(candidate), reg));
                }
            }
        }
        return out;
    }

    // ---- Server: buttons ----

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide()) {
            return true;
        }
        owner = player;
        if (id >= BTN_SET) {
            int code = id - BTN_SET;
            int slot = code / 10000;
            int raw = code % 10000;
            if (kind != Kind.STORAGE && slot >= 0 && slot < GRID) {
                Item item = BuiltInRegistries.ITEM.byId(raw);
                grid.setItem(slot, raw == 0 || item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
                updateResult();
            }
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
        if (id >= BTN_MODE && id <= BTN_GRID_CLEAR) {
            if (kind == Kind.PATTERN) {
                switch (id) {
                    case BTN_MODE -> {
                        processing = !processing;
                        updateResult();
                    }
                    case BTN_TAGS -> tags = !tags;
                    case BTN_ENCODE -> encode();
                    default -> {
                        for (int i = 0; i < GRID; i++) {
                            grid.setItem(i, ItemStack.EMPTY);
                        }
                        for (int i = 0; i < OUTS; i++) {
                            outs.setItem(i, ItemStack.EMPTY);
                        }
                        updateResult();
                    }
                }
            } else if (id == BTN_GRID_CLEAR && kind == Kind.CRAFTING) {
                for (int i = 0; i < GRID; i++) {
                    grid.setItem(i, ItemStack.EMPTY);
                }
                updateResult();
            }
            dirty = true;
            return true;
        }
        if (id >= BTN_HEIGHT && id < BTN_HEIGHT + 3) {
            if (player instanceof ServerPlayer sp && pos != null) {
                TerminalMenus.setHeight(sp, id - BTN_HEIGHT);
                TerminalMenus.open(sp, level, pos, kind);
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

    // ---- Server: clicks ----

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < view) {
            if (level != null && !level.isClientSide() && pos != null && player instanceof ServerPlayer sp) {
                owner = player;
                listClick(slotIndex, button, input, sp);
            }
            return;
        }
        if (gridSlot(slotIndex) || outputSlot(slotIndex)) {
            ghostClick(slotIndex, button);
            return;
        }
        if (kind != Kind.STORAGE && slotIndex == resultSlot) {
            if (kind == Kind.CRAFTING && level != null && !level.isClientSide() && pos != null && player instanceof ServerPlayer sp) {
                owner = player;
                craftClick(input, sp);
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
                    moveItemStackTo(out, firstPlayerSlot, slots.size(), true);
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
        if (level == null || level.isClientSide() || pos == null) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index < firstPlayerSlot) {
            if (kind == Kind.PATTERN && index >= resultSlot + 1) {
                ItemStack stack = slot.getItem();
                ItemStack copy = stack.copy();
                if (moveItemStackTo(stack, firstPlayerSlot, slots.size(), true)) {
                    slot.setChanged();
                    return copy;
                }
            }
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (kind == Kind.PATTERN && stack.getItem() instanceof PatternItem p && p.type() == PatternItem.Type.BLANK) {
            ItemStack copy = stack.copy();
            if (moveItemStackTo(stack, resultSlot + 1, resultSlot + 2, false)) {
                slot.setChanged();
                return copy;
            }
        }
        StorageNetwork net = StorageNetwork.find(level, pos);
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
    public void removed(Player player) {
        super.removed(player);
        if (level != null && !level.isClientSide() && kind == Kind.PATTERN) {
            clearContainer(player, blank);
            clearContainer(player, encoded);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
