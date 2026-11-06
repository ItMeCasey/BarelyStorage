package com.itmecasey.barelystorage.dev;

import com.itmecasey.barelystorage.Dev;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.Pattern;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.AssemblerBlockEntity;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import com.itmecasey.barelystorage.item.PatternItem;
import com.itmecasey.barelystorage.menu.PatternMenu;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only camera tour: opens a fresh superflat world, builds a small server room, and saves screenshots of the blocks
 * and every screen into {@code run/screenshots} as {@code barelystorage_<name>.png}, then quits. Does nothing unless the
 * environment variable {@code BARELYSTORAGE_SHOTS} is set. It never touches the real mouse or keyboard: it calls the game's own
 * handlers (screen character input, container clicks) from inside the game.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyStorage");
    private static final boolean ENABLED = System.getenv("BARELYSTORAGE_SHOTS") != null;
    private static final boolean PLAY = "play".equals(System.getenv("BARELYSTORAGE_SHOTS"));
    private static final String WORLD = "barelystorage_shots";
    private static int refillTimer;

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int ground;
    private static boolean pauseOnLostFocus;

    private ShotTour() {
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!PLAY && minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        switch (stage) {
            case WAITING_FOR_MENU -> waitForMenu(minecraft);
            case LOADING_WORLD -> waitForWorld(minecraft);
            case TOURING -> {
                runSteps();
                if (PLAY) {
                    refill();
                }
            }
            default -> {
            }
        }
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("BARELYSTORAGE-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(PLAY ? Difficulty.PEACEFUL : Difficulty.NORMAL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("BARELYSTORAGE-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        LOGGER.info("BARELYSTORAGE-SHOTS in the world, ground at y={}", ground);
        plan();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // ---- The tour ----

    private static void put(String block, int x, int y, int z, String props) {
        server(String.format(Locale.ROOT, "setblock %d %d %d barelystorage:%s%s", x, y, z, block, props));
    }

    private static void plan() {
        int y = ground;
        then(0, () -> {
            server("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
                    "difficulty peaceful", "time set noon", "weather clear", "kill @e[type=!player,type=!item]", "clear @p");
            player().getAbilities().flying = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        // The server room: rack, cable, storage terminal, cable, pattern terminal, cable, assembler.
        then(4, () -> {
            server("fill -3 " + (y - 1) + " -3 14 " + (y - 1) + " 4 minecraft:smooth_stone");
            put("server_rack", 0, y, 0, "[facing=north]");
            put("rack_bus_cable", 1, y, 0, "");
            put("storage_terminal", 2, y, 0, "[facing=north]");
            put("rack_bus_cable", 3, y, 0, "");
            put("pattern_terminal", 4, y, 0, "[facing=north]");
            put("rack_bus_cable", 5, y, 0, "");
            put("assembler", 6, y, 0, "");
            put("server_rack", 0, y + 1, 0, "[facing=north]");
            put("rack_bus_cable", 1, y + 1, 0, "");
            put("storage_bus", 2, y + 1, 0, "[facing=east]");
            server("setblock 3 " + (y + 1) + " 0 minecraft:chest");
            // Interface and import nodes around a furnace.
            put("rack_bus_cable", 8, y, 0, "");
            put("rack_bus_cable", 8, y + 1, 0, "");
            put("rack_bus_cable", 8, y + 2, 0, "");
            server("setblock 9 " + (y + 1) + " 0 minecraft:furnace[facing=north]");
            put("interface_node", 9, y + 2, 0, "[facing=down]");
            put("import_node", 9, y, 0, "[facing=up]");
            put("rack_bus_cable", 7, y, 0, "");
        });
        then(10, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            RackBlockEntity r = (RackBlockEntity) level.getBlockEntity(new BlockPos(0, y, 0));
            r.drives.setItem(0, new ItemStack(ModContent.item("cartridge_obsidian")));
            r.drives.setItem(1, new ItemStack(ModContent.item("cartridge_gold")));
            r.drives.setItem(2, new ItemStack(ModContent.item("cartridge_steel")));
            r.drives.setItem(3, new ItemStack(ModContent.item("cartridge_iron")));
            r.blades.setItem(0, new ItemStack(ModContent.item("basic_core")));
            r.setEnergy(r.maxEnergy() * 3 / 4);
            RackBlockEntity top = (RackBlockEntity) level.getBlockEntity(new BlockPos(0, y + 1, 0));
            top.drives.setItem(0, new ItemStack(ModContent.item("cartridge_quantum")));
            top.setEnergy(top.maxEnergy() / 2);
        }));
        then(30, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            // Distinct items with varied counts for the terminal (the racks have ticked online by now).
            StorageNetwork net = StorageNetwork.find(level, new BlockPos(0, y, 0));
            List<Item> pool = List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.COBBLESTONE, Items.OAK_PLANKS, Items.REDSTONE,
                    Items.COAL, Items.COPPER_INGOT, Items.LAPIS_LAZULI, Items.QUARTZ, Items.STICK, Items.OAK_LOG, Items.GLASS, Items.SAND,
                    Items.GRAVEL, Items.STONE, Items.DIRT, Items.EMERALD, Items.OBSIDIAN, Items.BLAZE_ROD, Items.ENDER_PEARL, Items.BONE,
                    Items.STRING, Items.LEATHER, Items.WHEAT, Items.CARROT, Items.APPLE, Items.BREAD, Items.IRON_PICKAXE, Items.DIAMOND_SWORD,
                    Items.GOLD_BLOCK, Items.IRON_BLOCK, Items.HOPPER, Items.CHEST, Items.PAPER, Items.SLIME_BALL, Items.GUNPOWDER,
                    Items.NETHERITE_SCRAP, Items.AMETHYST_SHARD, Items.GLOWSTONE_DUST, Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER);
            int i = 0;
            for (Item item : pool) {
                int count = new int[] {1280, 64, 7, 5000, 320, 903, 15, 2048, 40, 99, 9}[i % 11] * (1 + i % 3);
                net.insert(new ItemStack(item, count));
                i++;
            }
            AssemblerBlockEntity a = (AssemblerBlockEntity) level.getBlockEntity(new BlockPos(6, y, 0));
            a.patterns.setItem(0, encoded(Pattern.Kind.CRAFTING, new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.OAK_LOG)));
            a.patterns.setItem(1, encoded(Pattern.Kind.CRAFTING, new ItemStack(Items.STICK, 4), new ItemStack(Items.OAK_PLANKS, 2)));
            a.patterns.setItem(2, encoded(Pattern.Kind.CRAFTING, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.STICK, 2)));
            if (level.getBlockEntity(new BlockPos(9, y + 2, 0)) instanceof com.itmecasey.barelystorage.block.InterfaceNodeBlockEntity node) {
                node.patterns.setItem(0, encoded(Pattern.Kind.PROCESSING, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.RAW_IRON)));
            }
            a.patterns.setItem(4, encoded(Pattern.Kind.CRAFTING, new ItemStack(Items.OAK_SLAB, 6), new ItemStack(Items.OAK_PLANKS, 3)));
            a.patterns.setItem(5, encoded(Pattern.Kind.CRAFTING, new ItemStack(Items.BARREL), new ItemStack(Items.OAK_PLANKS, 6), new ItemStack(Items.OAK_SLAB, 2)));
            LOGGER.info("BARELYSTORAGE-SHOTS stocked: {} items types", net.pool().view().size());
        }));
        if (PLAY) {
            then(10, () -> {
                StringBuilder sb = new StringBuilder();
                for (String name : ModContent.items().keySet()) {
                    server("give @p barelystorage:" + name + (name.contains("pattern") ? " 16" : name.equals("rack_bus_cable") ? " 64" : ""));
                }
                server("give @p minecraft:iron_ingot 64", "give @p minecraft:redstone 64", "give @p minecraft:raw_iron 64", "give @p minecraft:coal 64",
                        "give @p minecraft:oak_log 64", "give @p minecraft:oak_planks 64", "give @p minecraft:cobblestone 64", "give @p minecraft:furnace 4",
                        "give @p minecraft:chest 4", "give @p minecraft:hopper 4", "give @p minecraft:diamond 32", "give @p minecraft:gold_ingot 64");
                server("tp @a 4.5 " + y + " -4 0 12", "gamemode creative @a");
                LOGGER.info("BARELYSTORAGE-SHOTS play mode ready");
            });
            return;
        }
        then(30, () -> moveTo(new Vec3(4.0, y + 2.4, -8.0), new Vec3(4.0, y + 1.2, 0.5)));
        shoot("room_overview", 30);
        then(2, () -> moveTo(new Vec3(0.5, y + 1.6, -3.0), new Vec3(0.5, y + 1.0, 0.5)));
        shoot("rack_front", 20);
        then(2, () -> moveTo(new Vec3(3.0, y + 0.9, -2.6), new Vec3(3.0, y + 0.5, 0.5)));
        shoot("terminals_front", 20);
        then(2, () -> moveTo(new Vec3(6.0, y + 1.2, -2.6), new Vec3(6.0, y + 0.6, 0.5)));
        shoot("assembler_front", 20);
        then(2, () -> moveTo(new Vec3(9.3, y + 1.6, -3.4), new Vec3(9.0, y + 1.0, 0.5)));
        shoot("nodes_with_furnace", 20);
        then(2, () -> moveTo(new Vec3(2.5, y + 2.4, -3.2), new Vec3(2.5, y + 1.0, 0.5)));
        shoot("bus_and_cables", 20);

        // Screens.
        then(2, () -> moveTo(new Vec3(0.5, y + 1.0, -2.0), new Vec3(0.5, y + 0.5, 0.5)));
        then(5, () -> open(new BlockPos(0, y, 0)));
        shoot("gui_rack", 30);
        then(2, () -> close());
        then(10, () -> open(new BlockPos(2, y, 0)));
        shoot("gui_terminal", 40);
        shoot("gui_terminal_glyphs", 6);
        then(2, () -> clickPanel(-13, 5 + 80 + 9));
        then(2, () -> clickPanel(-13, 5 + 80 + 9));
        shoot("gui_terminal_craftable_view", 20);
        then(2, () -> clickPanel(8 + 8, 34 + 8));
        shoot("gui_terminal_request", 10);
        then(2, () -> clickPanel(27 + 4 + 21 * 5 + 10, 52 + 38 + 7));
        shoot("gui_terminal_request_plus_100", 6);
        then(2, () -> clickPanel(27 + 66 + 30, 52 + 58 + 8));
        then(2, () -> typeIntoScreen("ingot"));
        shoot("gui_terminal_search", 30);
        then(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.screen().onClose();
        });
        then(10, () -> open(new BlockPos(4, y, 0)));
        then(10, () -> onServer(server -> fillPatternGrid(server)));
        shoot("gui_pattern_terminal", 30);
        then(2, () -> close());
        then(10, () -> open(new BlockPos(6, y, 0)));
        shoot("gui_assembler", 30);
        then(2, () -> close());
        then(2, () -> moveTo(new Vec3(9.3, y + 1.6, -2.5), new Vec3(9.3, y + 1.0, 0.5)));
        then(10, () -> open(new BlockPos(9, y + 2, 0)));
        shoot("gui_interface", 30);
        then(2, () -> close());
        then(10, () -> open(new BlockPos(9, y, 0)));
        shoot("gui_import", 30);
        then(2, () -> close());
        then(2, () -> moveTo(new Vec3(2.5, y + 1.6, -2.5), new Vec3(2.5, y + 1.0, 0.5)));
        then(10, () -> open(new BlockPos(2, y + 1, 0)));
        shoot("gui_bus", 30);
        then(2, () -> close());
        then(2, () -> moveTo(new Vec3(0.5, y + 1.0, -2.0), new Vec3(0.5, y + 0.5, 0.5)));
        // A deep job on a basic core: Thread Exhaustion shows on the rack screen.
        then(5, () -> onServer(server -> {
            ServerLevel level = server.overworld();
            StorageNetwork net = StorageNetwork.find(level, new BlockPos(0, y, 0));
            net.requestCraft(com.itmecasey.barelystorage.Keys.of(new ItemStack(Items.BARREL), level.registryAccess()), 4);
        }));
        then(30, () -> open(new BlockPos(0, y, 0)));
        shoot("gui_rack_halted", 40);
        then(2, () -> close());
        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("BARELYSTORAGE-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    /** Play mode: keeps the two racks charged so the room keeps working while someone plays. */
    private static void refill() {
        if (++refillTimer % 100 != 0) {
            return;
        }
        server("kill @e[type=!player,type=!item]", "time set noon", "weather clear");
        onServer(server -> {
            ServerLevel level = server.overworld();
            for (int dy = 0; dy <= 1; dy++) {
                if (level.getBlockEntity(new BlockPos(0, ground + dy, 0)) instanceof RackBlockEntity r) {
                    r.setEnergy(r.maxEnergy());
                }
            }
        });
    }

    private static ItemStack encoded(Pattern.Kind kind, ItemStack out, ItemStack... in) {
        var reg = Minecraft.getInstance().getSingleplayerServer().registryAccess();
        List<Pattern.Stack> inputs = new java.util.ArrayList<>();
        for (ItemStack s : in) {
            inputs.add(new Pattern.Stack(com.itmecasey.barelystorage.Keys.of(s, reg), s.getCount()));
        }
        ItemStack stack = new ItemStack(ModContent.item(kind == Pattern.Kind.CRAFTING ? "crafting_pattern" : "processing_pattern"));
        PatternItem.write(stack, new Pattern(kind, inputs, List.of(new Pattern.Stack(com.itmecasey.barelystorage.Keys.of(out, reg), out.getCount()))));
        return stack;
    }

    private static void fillPatternGrid(MinecraftServer server) {
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        if (!(p.containerMenu instanceof PatternMenu menu)) {
            return;
        }
        Item[] grid = {Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, null, Items.STICK, null, null, Items.STICK, null};
        for (int i = 0; i < 9; i++) {
            if (grid[i] != null) {
                menu.setCarried(new ItemStack(grid[i]));
                menu.clicked(i, 0, ContainerInput.PICKUP, p);
            }
        }
        menu.setCarried(ItemStack.EMPTY);
        menu.broadcastChanges();
    }

    private static void open(BlockPos pos) {
        onServer(server -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            ServerLevel level = server.overworld();
            var result = level.getBlockState(pos).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
            LOGGER.info("BARELYSTORAGE-SHOTS open {} block={} result={} menu={}", pos, level.getBlockState(pos).getBlock().getDescriptionId(), result, p.containerMenu.getClass().getSimpleName());
        });
    }

    private static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null) {
            minecraft.gui.screen().onClose();
        }
    }

    /** Clicks the open screen at a position relative to the top left of a 184 x 222 panel (a Storage Terminal at its smallest height). */
    private static void clickPanel(double dx, double dy) {
        Minecraft minecraft = Minecraft.getInstance();
        net.minecraft.client.gui.screens.Screen screen = minecraft.gui.screen();
        if (screen == null) {
            return;
        }
        double x = (screen.width - 184) / 2.0 + dx;
        double y = (screen.height - 222) / 2.0 + dy;
        net.minecraft.client.input.MouseButtonEvent event = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void typeIntoScreen(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null) {
            for (char c : text.toCharArray()) {
                minecraft.gui.screen().charTyped(new CharacterEvent(c));
            }
        }
    }

    static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.hud.getChat().clearMessages(false);
            minecraft.gui.toastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("BARELYSTORAGE-SHOTS {}", name);
            Screenshot.grab(minecraft.gameDirectory, "barelystorage_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> LOGGER.info("BARELYSTORAGE-SHOTS saved {}", name));
        });
    }

    static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    static void server(String... commands) {
        onServer(server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        });
    }

    static {
        if (Dev.ENABLED && ENABLED) {
            LOGGER.info("BARELYSTORAGE-SHOTS enabled");
        }
    }
}
