package com.itmecasey.barelystorage.dev;

import com.itmecasey.barelystorage.CraftScheduler;
import com.itmecasey.barelystorage.Dev;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.ExportNodeBlockEntity;
import com.itmecasey.barelystorage.block.ImportNodeBlockEntity;
import com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity;
import com.itmecasey.barelystorage.block.NodeBlock;
import com.itmecasey.barelystorage.block.RackBlock;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.block.StorageBusBlockEntity;
import com.itmecasey.barelystorage.item.CartridgeItem;
import com.itmecasey.barelystorage.item.PatternItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the whole network: power, storage, persistence, linking racks, Storage Bus, Interface and Import nodes with a
 * real furnace, crafting through an Assembler, and the Thread Exhaustion / Out of Memory halt. Runs once at server start when
 * {@code BARELYSTORAGE_SELFTEST} is set and prints {@code SELFTEST PASS/FAIL} lines ending in {@code SELFTEST DONE failures=N}.
 * It ticks the block entities by hand, so it needs no running game loop.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyStorage");
    private static ServerLevel level;
    private static int failures;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("BARELYSTORAGE_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer server) {
        level = server.overworld();
        failures = 0;
        BlockPos spawn = level.getRespawnData().pos();
        int x = spawn.getX() + 3;
        int z = spawn.getZ() + 3;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 25;
        try {
            powerAndStorage(new BlockPos(x, y, z));
            twoRacks(new BlockPos(x, y, z + 6));
            storageBus(new BlockPos(x, y, z + 12));
            furnaceProcessing(new BlockPos(x, y + 6, z));
            craftingAndHalt(new BlockPos(x, y + 6, z + 8));
            filtersAndUpgrades(new BlockPos(x, y + 12, z));
            terminalClicks(new BlockPos(x, y + 18, z));
            efficiency(new BlockPos(x, y + 24, z));
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error", e);
            verdict("selftest_ran_clean", false, e.toString());
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    // ---- helpers ----

    private static void place(BlockPos pos, String block) {
        level.setBlock(pos, ModContent.blocks().get(block).defaultBlockState(), 3);
    }

    private static void placeNode(BlockPos pos, String block, Direction facing) {
        level.setBlock(pos, ModContent.blocks().get(block).defaultBlockState().setValue(NodeBlock.FACING, facing), 3);
    }

    private static RackBlockEntity rack(BlockPos pos) {
        place(pos, "server_rack");
        return (RackBlockEntity) level.getBlockEntity(pos);
    }

    private static void tick(RackBlockEntity r) {
        RackBlockEntity.serverTick(level, r.getBlockPos(), level.getBlockState(r.getBlockPos()), r);
    }

    private static ItemKey key(net.minecraft.world.item.Item item) {
        return Keys.of(new ItemStack(item), level.registryAccess());
    }

    private static StorageNetwork net(BlockPos pos) {
        return StorageNetwork.find(level, pos);
    }

    private static void clear(BlockPos from, BlockPos to) {
        for (BlockPos p : BlockPos.betweenClosed(from, to)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    private static ItemStack cart(String id) {
        return new ItemStack(ModContent.item(id));
    }

    // ---- tests ----

    private static void powerAndStorage(BlockPos p) {
        clear(p.offset(-1, -1, -1), p.offset(2, 1, 1));
        RackBlockEntity r = rack(p);
        r.drives.setItem(0, cart("cartridge_iron"));
        tick(r);
        verdict("rack_without_power_is_offline", !r.online() && !net(p).online(), "online=" + r.online());
        ItemStack rest = net(p).insert(new ItemStack(Items.COBBLESTONE, 10));
        verdict("offline_network_stores_nothing", rest.getCount() == 10, "rest=" + rest.getCount());
        r.setEnergy(r.maxEnergy());
        tick(r);
        verdict("rack_with_power_goes_online", r.online() && level.getBlockState(p).getValue(RackBlock.ONLINE), "energy=" + r.energy());
        ItemStack left = net(p).insert(new ItemStack(Items.COBBLESTONE, 64));
        verdict("insert_into_cartridge", left.isEmpty() && net(p).pool().count(key(Items.COBBLESTONE)) == 64, "count=" + net(p).pool().count(key(Items.COBBLESTONE)));
        ItemStack out = net(p).extract(key(Items.COBBLESTONE), 32);
        ItemStack drive = r.drives.getItem(0);
        long inItem = CartridgeItem.read(drive, ((CartridgeItem) drive.getItem()).tier()).count(key(Items.COBBLESTONE));
        verdict("extract_updates_cartridge_item", out.getCount() == 32 && inItem == 32, "out=" + out.getCount() + " item=" + inItem);
        int before = r.energy();
        for (int i = 0; i < 20; i++) {
            tick(r);
        }
        verdict("rack_uses_power_each_tick", r.energy() == before - 20 * r.upkeep(), "used=" + (before - r.energy()) + " upkeep=" + r.upkeep());
        CompoundTag saved = r.saveWithoutMetadata(level.registryAccess());
        RackBlockEntity copy = new RackBlockEntity(p, level.getBlockState(p));
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved));
        verdict("rack_survives_saving_and_loading", copy.energy() == r.energy() && copy.cart(0) != null && copy.cart(0).count(key(Items.COBBLESTONE)) == 32,
                "energy=" + copy.energy() + " cobble=" + (copy.cart(0) == null ? -1 : copy.cart(0).count(key(Items.COBBLESTONE))));
        r.setEnergy(0);
        tick(r);
        verdict("power_loss_takes_network_offline", !r.online() && !net(p).online() && net(p).insert(new ItemStack(Items.DIRT, 5)).getCount() == 5, "online=" + r.online());
        clear(p.offset(-1, -1, -1), p.offset(2, 1, 1));
    }

    private static void twoRacks(BlockPos p) {
        clear(p.offset(-1, -1, -1), p.offset(4, 1, 1));
        RackBlockEntity a = rack(p);
        place(p.east(), "rack_bus_cable");
        RackBlockEntity b = rack(p.east(2));
        a.drives.setItem(0, cart("cartridge_iron"));
        b.drives.setItem(0, cart("cartridge_iron"));
        a.setEnergy(a.maxEnergy());
        b.setEnergy(b.maxEnergy());
        tick(a);
        tick(b);
        StorageNetwork n = net(p);
        verdict("bus_cable_links_two_racks", n.racks.size() == 2 && net(p.east(2)).racks.size() == 2, "racks=" + n.racks.size());
        ItemStack rest = net(p).insert(new ItemStack(Items.IRON_INGOT, 64));
        for (int i = 0; i < 130; i++) {
            rest = net(p).insert(new ItemStack(Items.IRON_INGOT, 64));
        }
        long total = a.used() + b.used();
        verdict("items_spread_over_linked_racks", total == 131 * 64 && net(p).pool().count(key(Items.IRON_INGOT)) == total, "used=" + total);
        b.setEnergy(0);
        tick(b);
        verdict("unpowered_rack_hides_its_drives", net(p).pool().count(key(Items.IRON_INGOT)) == a.used(), "visible=" + net(p).pool().count(key(Items.IRON_INGOT)));
        clear(p.offset(-1, -1, -1), p.offset(4, 1, 1));
    }

    private static void storageBus(BlockPos p) {
        clear(p.offset(-1, -1, -1), p.offset(5, 1, 1));
        RackBlockEntity r = rack(p);
        r.drives.setItem(0, cart("cartridge_iron"));
        r.setEnergy(r.maxEnergy());
        tick(r);
        place(p.east(), "rack_bus_cable");
        placeNode(p.east(2), "storage_bus", Direction.EAST);
        level.setBlock(p.east(3), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(p.east(3));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 10));
        StorageBusBlockEntity bus = (StorageBusBlockEntity) level.getBlockEntity(p.east(2));
        long seen = net(p).pool().count(key(Items.DIAMOND));
        verdict("storage_bus_maps_chest_contents", seen == 10, "diamonds=" + seen);
        ItemStack took = net(p).extract(key(Items.DIAMOND), 3);
        verdict("extract_through_bus_leaves_chest_updated", took.getCount() == 3 && chest.countItem(Items.DIAMOND) == 7, "chest=" + chest.countItem(Items.DIAMOND));
        bus.cyclePriority();
        bus.cyclePriority();
        net(p).insert(new ItemStack(Items.DIRT, 5));
        verdict("high_priority_bus_receives_first", chest.countItem(Items.DIRT) == 5, "dirt in chest=" + chest.countItem(Items.DIRT));
        clear(p.offset(-1, -1, -1), p.offset(5, 1, 1));
    }

    private static Pattern pattern(Pattern.Kind kind, ItemStack out, ItemStack... in) {
        List<Pattern.Stack> inputs = new java.util.ArrayList<>();
        for (ItemStack s : in) {
            inputs.add(new Pattern.Stack(Keys.of(s, level.registryAccess()), s.getCount()));
        }
        return new Pattern(kind, inputs, List.of(new Pattern.Stack(Keys.of(out, level.registryAccess()), out.getCount())));
    }

    private static ItemStack encoded(Pattern p) {
        ItemStack s = new ItemStack(ModContent.item(p.kind() == Pattern.Kind.CRAFTING ? "crafting_pattern" : "processing_pattern"));
        PatternItem.write(s, p);
        return s;
    }

    private static void furnaceProcessing(BlockPos p) {
        // Rack (0), cable column at x+1 from y-1 to y+1, furnace at x+2, Interface above it, Import below it, Assembler at z+1.
        clear(p.offset(-1, -2, -1), p.offset(4, 2, 2));
        RackBlockEntity r = rack(p);
        r.blades.setItem(0, new ItemStack(ModContent.item("basic_core")));
        r.drives.setItem(0, cart("cartridge_iron"));
        r.setEnergy(r.maxEnergy());
        for (int dy = -1; dy <= 1; dy++) {
            place(p.east().above(dy), "rack_bus_cable");
        }
        level.setBlock(p.east(2), Blocks.FURNACE.defaultBlockState(), 3);
        placeNode(p.east(2).above(), "interface_node", Direction.DOWN);
        placeNode(p.east(2).below(), "import_node", Direction.UP);
        place(p.east().south(), "assembler");
        AssemblerBlockEntity asm = (AssemblerBlockEntity) level.getBlockEntity(p.east().south());
        FurnaceBlockEntity furnace = (FurnaceBlockEntity) level.getBlockEntity(p.east(2));
        furnace.setItem(1, new ItemStack(Items.COAL, 1));
        InterfaceNodeBlockEntity iface = (InterfaceNodeBlockEntity) level.getBlockEntity(p.east(2).above());
        ImportNodeBlockEntity imp = (ImportNodeBlockEntity) level.getBlockEntity(p.east(2).below());
        iface.patterns.setItem(0, encoded(pattern(Pattern.Kind.PROCESSING, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.RAW_IRON))));
        tick(r);
        net(p).insert(new ItemStack(Items.RAW_IRON, 3));
        var plan = net(p).requestCraft(key(Items.IRON_INGOT), 2);
        verdict("processing_job_is_planned", plan.ok(), plan.message);
        for (int i = 0; i < 1500 && !r.scheduler().jobs().isEmpty(); i++) {
            tick(r);
            InterfaceNodeBlockEntity.serverTick(level, iface.getBlockPos(), level.getBlockState(iface.getBlockPos()), iface);
            ImportNodeBlockEntity.serverTick(level, imp.getBlockPos(), level.getBlockState(imp.getBlockPos()), imp);
            AssemblerBlockEntity.serverTick(level, asm.getBlockPos(), level.getBlockState(asm.getBlockPos()), asm);
            AbstractFurnaceBlockEntity.serverTick(level, furnace.getBlockPos(), level.getBlockState(furnace.getBlockPos()), furnace);
        }
        long ingots = net(p).pool().count(key(Items.IRON_INGOT));
        verdict("interface_feeds_furnace_and_import_returns_products", ingots == 2 && r.scheduler().jobs().isEmpty(),
                "ingots=" + ingots + " jobs=" + r.scheduler().jobs().size() + " raw=" + net(p).pool().count(key(Items.RAW_IRON))
                        + " furnace=[" + furnace.getItem(0) + "," + furnace.getItem(1) + "," + furnace.getItem(2) + "] buffer=" + iface.buffer.getItem(0)
                        + " steps=" + (r.scheduler().jobs().isEmpty() ? "-" : r.scheduler().jobs().get(0).steps.get(0).pending + "/" + r.scheduler().jobs().get(0).steps.get(0).active)
                        + " err=" + r.scheduler().lastError());
        clear(p.offset(-1, -2, -1), p.offset(4, 2, 2));
    }

    /** Clicks the Storage Terminal list the way a player does and checks the network really loses what the player gains. */
    private static void terminalClicks(BlockPos p) {
        clear(p.offset(-1, -1, -2), p.offset(4, 1, 2));
        RackBlockEntity r = rack(p);
        r.drives.setItem(0, cart("cartridge_gold"));
        r.setEnergy(r.maxEnergy());
        place(p.east(), "storage_terminal");
        tick(r);
        net(p).insert(new ItemStack(Items.COBBLESTONE, 100));
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "selftest"), net.minecraft.server.level.ClientInformation.createDefault());
        com.itmecasey.barelystorage.menu.TerminalMenu menu = new com.itmecasey.barelystorage.menu.TerminalMenu(com.itmecasey.barelystorage.menu.TerminalMenu.Kind.STORAGE, 5, 1,
                player.getInventory(), level, p.east());
        menu.broadcastChanges();
        int slot = -1;
        for (int i = 0; i < menu.viewSlots(); i++) {
            if (menu.slots.get(i).getItem().is(Items.COBBLESTONE)) {
                slot = i;
            }
        }
        verdict("terminal_list_shows_the_stored_item", slot >= 0 && menu.countAt(slot) == 100, "slot=" + slot + " shown=" + (slot >= 0 ? menu.countAt(slot) : -1) + " pool=" + net(p).pool().count(key(Items.COBBLESTONE)));
        long before = net(p).pool().count(key(Items.COBBLESTONE));
        menu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        long afterShift = net(p).pool().count(key(Items.COBBLESTONE));
        int held = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.COBBLESTONE)) {
                held += player.getInventory().getItem(i).getCount();
            }
        }
        verdict("shift_click_takes_a_stack_out_of_the_network", before - afterShift == 64 && held == 64, "network " + before + " -> " + afterShift + ", player holds " + held);
        menu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        long afterClick = net(p).pool().count(key(Items.COBBLESTONE));
        verdict("single_click_takes_one_item_out_of_the_network", afterShift - afterClick == 1 && menu.getCarried().getCount() == 1,
                "network " + afterShift + " -> " + afterClick + ", carried " + menu.getCarried().getCount());
        menu.broadcastChanges();
        ItemStack driveStack = r.drives.getItem(0);
        long inItem = com.itmecasey.barelystorage.item.CartridgeItem.read(driveStack, ((com.itmecasey.barelystorage.item.CartridgeItem) driveStack.getItem()).tier()).count(key(Items.COBBLESTONE));
        int shownNow = 0;
        for (int i = 0; i < menu.viewSlots(); i++) {
            if (menu.slots.get(i).getItem().is(Items.COBBLESTONE)) {
                shownNow += (int) menu.countAt(i);
            }
        }
        verdict("taken_items_are_gone_from_the_cartridge_item_and_the_list", inItem == afterClick && shownNow == afterClick,
                "item=" + inItem + " pool=" + afterClick + " listShows=" + shownNow);
        clear(p.offset(-1, -1, -2), p.offset(4, 1, 2));
    }

    private static void efficiency(BlockPos p) {
        clear(p.offset(-1, -1, -2), p.offset(4, 1, 2));
        RackBlockEntity r = rack(p);
        r.drives.setItem(0, cart("cartridge_gold"));
        r.setEnergy(r.maxEnergy());
        place(p.east(), "rack_bus_cable");
        place(p.east(2), "assembler");
        tick(r);
        r.refreshEfficiency();
        int before = r.upkeep();
        AssemblerBlockEntity asm = (AssemblerBlockEntity) level.getBlockEntity(p.east(2));
        asm.upgrades.setItem(0, new ItemStack(ModContent.item("upgrade_efficiency")));
        asm.upgrades.setItem(1, new ItemStack(ModContent.item("upgrade_efficiency")));
        r.refreshEfficiency();
        int after = r.upkeep();
        verdict("two_efficiency_cards_lower_the_rack_upkeep_by_a_fifth", after == before - before * 20 / 100 && after < before, "upkeep " + before + " -> " + after);
        clear(p.offset(-1, -1, -2), p.offset(4, 1, 2));
    }

    private static void filtersAndUpgrades(BlockPos p) {
        clear(p.offset(-1, -1, -2), p.offset(6, 1, 2));
        RackBlockEntity r = rack(p);
        r.drives.setItem(0, cart("cartridge_gold"));
        r.setEnergy(r.maxEnergy());
        place(p.east(), "rack_bus_cable");
        placeNode(p.east(2), "export_node", Direction.EAST);
        level.setBlock(p.east(3), Blocks.CHEST.defaultBlockState(), 3);
        placeNode(p.east().north(), "import_node", Direction.NORTH);
        level.setBlock(p.east().north(2), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity source = (ChestBlockEntity) level.getBlockEntity(p.east().north(2));
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        source.setItem(1, new ItemStack(Items.GOLD_INGOT, 5));
        ImportNodeBlockEntity importer = (ImportNodeBlockEntity) level.getBlockEntity(p.east().north());
        importer.filter.setItem(0, new ItemStack(Items.IRON_INGOT));
        tick(r);
        net(p).insert(new ItemStack(Items.DIAMOND, 20));
        net(p).insert(new ItemStack(Items.EMERALD, 20));
        ExportNodeBlockEntity export = (ExportNodeBlockEntity) level.getBlockEntity(p.east(2));
        ChestBlockEntity out = (ChestBlockEntity) level.getBlockEntity(p.east(3));
        for (int i = 0; i < 40; i++) {
            ExportNodeBlockEntity.serverTick(level, export.getBlockPos(), level.getBlockState(export.getBlockPos()), export);
        }
        verdict("export_node_with_empty_filter_pushes_nothing", out.isEmpty(), "chest=" + out.countItem(Items.DIAMOND));
        export.filter.setItem(0, new ItemStack(Items.DIAMOND));
        for (int i = 0; i < 40; i++) {
            ExportNodeBlockEntity.serverTick(level, export.getBlockPos(), level.getBlockState(export.getBlockPos()), export);
        }
        verdict("export_node_pushes_only_filtered_items", out.countItem(Items.DIAMOND) == 20 && out.countItem(Items.EMERALD) == 0, "diamonds=" + out.countItem(Items.DIAMOND));
        for (int i = 0; i < 40; i++) {
            ImportNodeBlockEntity.serverTick(level, importer.getBlockPos(), level.getBlockState(importer.getBlockPos()), importer);
        }
        verdict("import_node_filter_takes_only_listed_items", source.countItem(Items.IRON_INGOT) == 0 && source.countItem(Items.GOLD_INGOT) == 5
                && net(p).pool().count(key(Items.IRON_INGOT)) == 5, "iron left=" + source.countItem(Items.IRON_INGOT) + " gold left=" + source.countItem(Items.GOLD_INGOT));
        importer.filter.setItem(0, ItemStack.EMPTY);
        for (int i = 0; i < 40; i++) {
            ImportNodeBlockEntity.serverTick(level, importer.getBlockPos(), level.getBlockState(importer.getBlockPos()), importer);
        }
        verdict("import_node_with_empty_filter_takes_everything", source.isEmpty(), "gold left=" + source.countItem(Items.GOLD_INGOT));
        long before = export.speed();
        export.upgrades.setItem(0, new ItemStack(ModContent.item("upgrade_speed")));
        export.filter.setItem(9, new ItemStack(Items.EMERALD));
        verdict("speed_card_raises_speed_and_filter_rows_are_locked_until_a_filter_card",
                before == 1 && export.speed() == 2 && export.filterRows() == 1 && export.filterKeys(level.registryAccess()).size() == 1, "speed=" + export.speed());
        export.upgrades.setItem(1, new ItemStack(ModContent.item("upgrade_filter")));
        verdict("filter_card_opens_a_row", export.filterRows() == 2 && export.filterKeys(level.registryAccess()).size() == 2, "rows=" + export.filterRows());
        clear(p.offset(-1, -1, -2), p.offset(6, 1, 2));
    }

    private static void craftingAndHalt(BlockPos p) {
        clear(p.offset(-1, -1, -1), p.offset(3, 1, 2));
        RackBlockEntity r = rack(p);
        r.blades.setItem(0, new ItemStack(ModContent.item("basic_core")));
        r.drives.setItem(0, cart("cartridge_iron"));
        r.setEnergy(r.maxEnergy());
        place(p.east(), "assembler");
        AssemblerBlockEntity asm = (AssemblerBlockEntity) level.getBlockEntity(p.east());
        asm.patterns.setItem(0, encoded(pattern(Pattern.Kind.CRAFTING, new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.OAK_LOG))));
        asm.patterns.setItem(1, encoded(pattern(Pattern.Kind.CRAFTING, new ItemStack(Items.STICK, 4), new ItemStack(Items.OAK_PLANKS, 2))));
        tick(r);
        net(p).insert(new ItemStack(Items.OAK_LOG, 1));
        var plan = net(p).requestCraft(key(Items.STICK), 4);
        verdict("chain_job_is_planned", plan.depth == 2 && plan.steps.size() == 2, "depth=" + plan.depth + " " + plan.message);
        verdict("basic_core_halts_deep_chain_with_thread_exhaustion",
                r.scheduler().halted() && r.scheduler().lastError().startsWith("Thread Exhaustion") && r.log().stream().anyMatch(l -> l.startsWith("Thread Exhaustion")),
                r.scheduler().lastError());
        for (int i = 0; i < 40; i++) {
            tick(r);
            AssemblerBlockEntity.serverTick(level, asm.getBlockPos(), level.getBlockState(asm.getBlockPos()), asm);
        }
        verdict("halted_queue_makes_no_progress", net(p).pool().count(key(Items.STICK)) == 0 && net(p).pool().count(key(Items.OAK_LOG)) == 1, "sticks=" + net(p).pool().count(key(Items.STICK)));
        r.blades.setItem(1, new ItemStack(ModContent.item("ram_blade")));
        for (int i = 0; i < 400 && !r.scheduler().jobs().isEmpty(); i++) {
            tick(r);
            AssemblerBlockEntity.serverTick(level, asm.getBlockPos(), level.getBlockState(asm.getBlockPos()), asm);
        }
        long sticks = net(p).pool().count(key(Items.STICK));
        long planks = net(p).pool().count(key(Items.OAK_PLANKS));
        verdict("installing_a_ram_blade_resumes_and_finishes", sticks == 4 && planks == 2 && r.scheduler().jobs().isEmpty() && r.scheduler().lastError().isEmpty(),
                "sticks=" + sticks + " planks=" + planks + " jobs=" + r.scheduler().jobs().size());
        Settings.set("hardware.basic_core.ram", 1);
        Settings.set("hardware.ram_blade.ram", 1);
        net(p).insert(new ItemStack(Items.OAK_LOG, 1));
        r.scheduler().setHardware(net(p).hardware(), net(p));
        net(p).requestCraft(key(Items.OAK_PLANKS), 4);
        verdict("too_little_ram_halts_with_out_of_memory", r.scheduler().halted() && r.scheduler().lastError().startsWith("Out of Memory"), r.scheduler().lastError());
        Settings.reset();
        r.scheduler().setHardware(net(p).hardware(), net(p));
        verdict("restoring_ram_clears_the_halt", !r.scheduler().halted(), r.scheduler().lastError());
        clear(p.offset(-1, -1, -1), p.offset(3, 1, 2));
    }
}
