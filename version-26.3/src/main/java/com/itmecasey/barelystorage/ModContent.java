package com.itmecasey.barelystorage;

import com.itmecasey.barelystorage.block.AssemblerBlock;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.CableBlock;
import com.itmecasey.barelystorage.block.ImportNodeBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.NodeBlock;
import com.itmecasey.barelystorage.block.RackBlock;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.block.TerminalBlock;
import com.itmecasey.barelystorage.item.BladeItem;
import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.item.PatternItem;
import com.itmecasey.barelystorage.menu.GridMenu;
import com.itmecasey.barelystorage.menu.PatternMenu;
import com.itmecasey.barelystorage.menu.RackMenu;
import com.itmecasey.barelystorage.menu.TerminalMenu;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item, block entity type and menu type the mod adds, built once and handed to the loader's registry event. */
public final class ModContent {
    public static final String MODID = BarelyStorage.MODID;

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final BlockEntityType<RackBlockEntity> RACK;
    public static final BlockEntityType<StorageBusBlockEntity> BUS;
    public static final BlockEntityType<InterfaceNodeBlockEntity> INTERFACE;
    public static final BlockEntityType<ImportNodeBlockEntity> IMPORT;
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER;
    public static final MenuType<RackMenu> RACK_MENU = new MenuType<>(RackMenu::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<TerminalMenu> TERMINAL_MENU = new MenuType<>(TerminalMenu::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<PatternMenu> PATTERN_MENU = new MenuType<>(PatternMenu::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<GridMenu> INTERFACE_MENU = new MenuType<>(GridMenu::clientInterface, FeatureFlags.VANILLA_SET);
    public static final MenuType<GridMenu> ASSEMBLER_MENU = new MenuType<>(GridMenu::clientAssembler, FeatureFlags.VANILLA_SET);

    static {
        RackBlock rack = new RackBlock(props("server_rack").mapColor(MapColor.METAL).strength(4.0F, 8.0F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().lightLevel(s -> s.getValue(RackBlock.ONLINE) ? 6 : 0));
        block("server_rack", rack);
        block("rack_bus_cable", new CableBlock(props("rack_bus_cable").mapColor(MapColor.COLOR_GRAY).strength(1.5F, 4.0F).sound(SoundType.METAL).noOcclusion()));
        NodeBlock bus = new NodeBlock(NodeBlock.Kind.BUS, node("storage_bus"));
        NodeBlock iface = new NodeBlock(NodeBlock.Kind.INTERFACE, node("interface_node"));
        NodeBlock imp = new NodeBlock(NodeBlock.Kind.IMPORT, node("import_node"));
        block("storage_bus", bus);
        block("interface_node", iface);
        block("import_node", imp);
        block("storage_terminal", new TerminalBlock(TerminalBlock.Kind.STORAGE, props("storage_terminal").mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 8)));
        block("pattern_terminal", new TerminalBlock(TerminalBlock.Kind.PATTERN, props("pattern_terminal").mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 8)));
        block("assembler", new AssemblerBlock(props("assembler").mapColor(MapColor.METAL).strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        RACK = new BlockEntityType<>(RackBlockEntity::new, Set.of(rack));
        BUS = new BlockEntityType<>(StorageBusBlockEntity::new, Set.of(bus));
        INTERFACE = new BlockEntityType<>(InterfaceNodeBlockEntity::new, Set.of(iface));
        IMPORT = new BlockEntityType<>(ImportNodeBlockEntity::new, Set.of(imp));
        ASSEMBLER = new BlockEntityType<>(AssemblerBlockEntity::new, Set.of(BLOCKS.get("assembler")));
        for (Tier t : Tier.values()) {
            item("cartridge_" + t.id(), new CartridgeItem(t, itemProps("cartridge_" + t.id())));
        }
        item("basic_core", new BladeItem(BladeItem.Kind.BASIC_CORE, itemProps("basic_core")));
        item("ram_blade", new BladeItem(BladeItem.Kind.RAM_BLADE, itemProps("ram_blade")));
        item("quantum_core", new BladeItem(BladeItem.Kind.QUANTUM_CORE, itemProps("quantum_core")));
        item("blank_pattern", new PatternItem(PatternItem.Type.BLANK, itemProps("blank_pattern")));
        item("crafting_pattern", new PatternItem(PatternItem.Type.CRAFTING, itemProps("crafting_pattern")));
        item("processing_pattern", new PatternItem(PatternItem.Type.PROCESSING, itemProps("processing_pattern")));
    }

    private ModContent() {
    }

    private static BlockBehaviour.Properties props(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)));
    }

    private static BlockBehaviour.Properties node(String name) {
        return props(name).mapColor(MapColor.METAL).strength(2.5F, 5.0F).sound(SoundType.METAL).noOcclusion();
    }

    private static void block(String name, Block block) {
        BLOCKS.put(name, block);
        ITEMS.put(name, new BlockItem(block, itemProps(name).useBlockDescriptionPrefix()));
    }

    private static void item(String name, Item item) {
        ITEMS.put(name, item);
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Item item(String name) {
        return ITEMS.get(name);
    }

    public static Map<String, Block> blocks() {
        return BLOCKS;
    }

    public static Map<String, Item> items() {
        return ITEMS;
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.barelystorage"))
                .icon(() -> new ItemStack(ITEMS.get("server_rack")))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }

    public static void registerBlockEntities(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept("server_rack", RACK);
        out.accept("storage_bus", BUS);
        out.accept("interface_node", INTERFACE);
        out.accept("import_node", IMPORT);
        out.accept("assembler", ASSEMBLER);
    }

    public static void registerMenus(BiConsumer<String, MenuType<?>> out) {
        out.accept("server_rack", RACK_MENU);
        out.accept("storage_terminal", TERMINAL_MENU);
        out.accept("pattern_terminal", PATTERN_MENU);
        out.accept("interface_node", INTERFACE_MENU);
        out.accept("assembler", ASSEMBLER_MENU);
    }
}
