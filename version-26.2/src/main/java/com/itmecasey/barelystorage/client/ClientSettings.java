package com.itmecasey.barelystorage.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.client.Minecraft;

/** The player's screen choices (theme, terminal height, sort and view), kept in config/barelystorage-client.properties. */
public final class ClientSettings {
    public static int theme;
    public static int height;
    public static int sort;
    public static int order;
    public static int view;
    private static boolean loaded;

    private ClientSettings() {
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("barelystorage-client.properties");
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                Properties p = new Properties();
                p.load(r);
                theme = clamp(p.getProperty("theme"), 1);
                height = clamp(p.getProperty("height"), 2);
                sort = clamp(p.getProperty("sort"), 2);
                order = clamp(p.getProperty("order"), 1);
                view = clamp(p.getProperty("view"), 2);
            } catch (IOException | RuntimeException e) {
                // Broken file: keep the defaults.
            }
        }
        Ui.theme(theme == 1);
    }

    public static void save() {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                w.write("# BarelyStorage screen settings, written by the game.\n");
                w.write("theme=" + theme + "\nheight=" + height + "\nsort=" + sort + "\norder=" + order + "\nview=" + view + "\n");
            }
        } catch (IOException e) {
            // Not being able to save a screen choice is not worth a crash.
        }
    }

    private static int clamp(String text, int max) {
        try {
            return Math.max(0, Math.min(max, Integer.parseInt(text.trim())));
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
