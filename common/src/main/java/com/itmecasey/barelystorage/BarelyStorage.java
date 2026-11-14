package com.itmecasey.barelystorage;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class BarelyStorage {
    public static final String MODID = "barelystorage";

    private BarelyStorage() {
    }

    /**
     * Runs before the loader reads its config files: on the first start with the new {@code .toml} files, the values of the
     * old {@code .properties} files are moved over (each old file is kept as {@code .back}).
     */
    public static void init(Supplier<Path> configDir) {
        Path dir = configDir.get();
        String version = ConfigMigration.modVersion();
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("barelystorage");
        ConfigMigration.seed(dir.resolve(ConfigMigration.COMMON_OLD), dir.resolve(ConfigMigration.COMMON_TOML), Settings.defs(), "BarelyStorage", version, log::info);
        ConfigMigration.seed(dir.resolve(ConfigMigration.CLIENT_OLD), dir.resolve(ConfigMigration.CLIENT_TOML), ConfigMigration.CLIENT, "BarelyStorage", version, log::info);
    }
}
