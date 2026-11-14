package com.itmecasey.barelystorage.script;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The running server as scripts see it, remembered from the first rack tick. */
public final class ScriptHost {
    private static volatile MinecraftServer server;

    private ScriptHost() {
    }

    public static void touch(Level level) {
        if (level.getServer() != null) {
            server = level.getServer();
        }
    }

    /** The name of a level's dimension, such as minecraft:overworld. */
    public static String dimension(Level level) {
        String text = String.valueOf(level.dimension());
        int slash = text.lastIndexOf("/ ");
        return slash >= 0 && text.endsWith("]") ? text.substring(slash + 2, text.length() - 1) : text;
    }

    @Nullable
    static ServerLevel level(String dimension) {
        MinecraftServer s = server;
        if (s == null) {
            return null;
        }
        for (ServerLevel l : s.getAllLevels()) {
            if (dimension(l).equals(dimension)) {
                return l;
            }
        }
        return null;
    }
}
