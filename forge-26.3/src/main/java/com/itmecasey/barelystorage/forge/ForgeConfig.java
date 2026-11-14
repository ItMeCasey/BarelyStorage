package com.itmecasey.barelystorage.forge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.ConfigMigration;
import com.itmecasey.barelystorage.Settings;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The mod's config files, {@code barelystorage-common.toml} (every number in {@link Settings}) and
 * {@code barelystorage-client.toml} (the screen choices). Both specs are built from the one list of keys, defaults, ranges and
 * descriptions in common code, and the loaded values are pushed into {@link Settings} whenever a file loads or reloads.
 */
@Mod.EventBusSubscriber(modid = BarelyStorage.MODID)
public final class ForgeConfig {
    private static final Map<String, ForgeConfigSpec.IntValue> COMMON_VALUES = new LinkedHashMap<>();
    private static final Map<String, ForgeConfigSpec.IntValue> CLIENT_VALUES = new LinkedHashMap<>();
    public static final ForgeConfigSpec COMMON;
    public static final ForgeConfigSpec CLIENT;

    static {
        COMMON = build(Settings.defs(), COMMON_VALUES);
        CLIENT = build(ConfigMigration.CLIENT, CLIENT_VALUES);
    }

    private ForgeConfig() {
    }

    private static ForgeConfigSpec build(java.util.List<Settings.Def> defs, Map<String, ForgeConfigSpec.IntValue> out) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        for (Settings.Def d : defs) {
            out.put(d.key(), builder.comment(d.comment()).defineInRange(d.key(), d.fallback(), d.min(), d.max()));
        }
        return builder.build();
    }

    public static void register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT);
    }

    /** The loaded client values in the order theme, height, sort, order, view. */
    public static int[] clientValues() {
        int[] out = new int[ConfigMigration.CLIENT.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = CLIENT_VALUES.get(ConfigMigration.CLIENT.get(i).key()).get();
        }
        return out;
    }

    /** Stores the client values in the same order and saves the file. */
    public static void storeClient(int[] values) {
        if (!CLIENT.isLoaded()) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            CLIENT_VALUES.get(ConfigMigration.CLIENT.get(i).key()).set(values[i]);
        }
        CLIENT.save();
    }

    private static void push(ModConfig config) {
        if (config.getSpec() == COMMON && COMMON.isLoaded()) {
            COMMON_VALUES.forEach((key, value) -> Settings.set(key, value.get()));
        } else if (config.getSpec() == CLIENT && CLIENT.isLoaded()) {
            ForgeClientBridge.apply(clientValues());
        }
    }

    @SubscribeEvent
    public static void loaded(ModConfigEvent.Loading event) {
        push(event.getConfig());
    }

    // The file watcher can report a reload after the config has unloaded; push() skips that.
    @SubscribeEvent
    public static void reloaded(ModConfigEvent.Reloading event) {
        push(event.getConfig());
    }
}
