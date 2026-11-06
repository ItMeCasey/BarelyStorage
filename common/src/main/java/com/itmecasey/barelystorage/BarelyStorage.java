package com.itmecasey.barelystorage;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class BarelyStorage {
    public static final String MODID = "barelystorage";

    private BarelyStorage() {
    }

    public static void init(Supplier<Path> configDir) {
        Settings.loadFile(configDir.get().resolve(MODID + ".properties"));
    }
}
