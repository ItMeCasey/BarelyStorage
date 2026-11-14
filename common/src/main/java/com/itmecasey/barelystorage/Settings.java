package com.itmecasey.barelystorage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Every tunable number in the mod. The file {@code config/barelystorage.properties} holds one {@code key=value} line per
 * setting; a missing key uses its default, and the file is rewritten with every key and a comment after loading.
 */
public final class Settings {
    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        def("power.buffer", 400000, "Energy (FE) a Server Rack can hold.");
        def("power.max_receive", 4000, "Most FE a Server Rack takes in per tick.");
        def("power.base_per_tick", 8, "FE per tick a powered rack always uses.");
        def("power.per_cartridge", 2, "Extra FE per tick for each cartridge in a rack.");
        def("power.per_blade", 4, "Extra FE per tick for each blade in a rack (a Quantum Core costs three times this).");
        def("power.efficiency_percent", 10, "Percent of a rack's upkeep each Efficiency card in the network takes off.");
        def("power.efficiency_max_percent", 50, "Most upkeep, in percent, Efficiency cards can take off.");

        def("tier.iron.capacity", 8192, "Items an Iron Drive holds.");
        def("tier.steel.capacity", 65536, "Items a Steel Drive holds.");
        def("tier.gold.capacity", 262144, "Items a Gold Drive holds.");
        def("tier.obsidian.capacity", 4194304, "Items an Obsidian Drive holds.");
        def("tier.quantum.capacity", 134217728, "Items a Quantum Drive holds.");
        def("tier.iron.types", 16, "Different item types an Iron Drive holds.");
        def("tier.steel.types", 32, "Different item types a Steel Drive holds.");
        def("tier.gold.types", 64, "Different item types a Gold Drive holds.");
        def("tier.obsidian.types", 128, "Different item types an Obsidian Drive holds.");
        def("tier.quantum.types", 512, "Different item types a Quantum Drive holds.");

        def("rack.drive_bays", 8, "Drive bays in a Server Rack (4 to 8).");
        def("rack.blade_slots", 4, "Blade slots in a Server Rack.");

        def("hardware.basic_core.threads", 1, "Threads one Basic Core adds.");
        def("hardware.basic_core.ram", 8, "RAM units one Basic Core adds.");
        def("hardware.ram_blade.threads", 4, "Threads one Multi-Threaded RAM Blade adds.");
        def("hardware.ram_blade.ram", 32, "RAM units one Multi-Threaded RAM Blade adds.");
        def("hardware.quantum_core.threads", 8, "Threads one Quantum Core adds.");
        def("hardware.quantum_core.ram", 128, "RAM units one Quantum Core adds.");
        def("hardware.ram_blade.depth", 3, "Deepest chain of patterns a rack with a RAM Blade but no Quantum Core can plan.");
        def("hardware.basic.depth", 1, "Deepest chain of patterns a rack with only Basic Cores can plan.");

        def("crafting.assembler_ticks_per_run", 10, "Ticks an Assembler needs for one run of a crafting pattern.");
        def("crafting.max_batch", 16, "Most runs of a pattern started at once.");
        def("crafting.job_limit", 16, "Most crafting jobs a network queues.");

        def("nodes.interval_ticks", 8, "Ticks between item moves of an Interface Node, Import Node or Storage Bus.");
        def("nodes.items_per_move", 32, "Most items an Interface or Import Node moves per step.");
    }

    private Settings() {
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Entry e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, e.fallback());
    }

    /** Every setting key, sorted. */
    public static java.util.List<String> keys() {
        return new java.util.ArrayList<>(new java.util.TreeSet<>(DEFS.keySet()));
    }

    /** The current value of a setting as text. */
    public static String value(String key) {
        return raw(key);
    }

    /** Overrides one setting (tests and scripts; not saved to the file). */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(DEFS.get(key).fallback());
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** Reads overrides from a properties text (also used by tests). */
    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        for (String key : p.stringPropertyNames()) {
            if (DEFS.containsKey(key)) {
                VALUES.put(key, p.getProperty(key));
            }
        }
    }

    /** Loads the file if there is one, then rewrites it with every key so players can see what they can change. */
    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            write(file);
        } catch (IOException | RuntimeException ex) {
            System.err.println("[BarelyStorage] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }

    private static void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("# BarelyStorage settings. Energy is in Forge Energy (FE); rates are per game tick (20 per second).\n");
            w.write("# Edit a value and restart the game or server. Delete a line to restore its default.\n");
            for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                w.write("\n# " + e.getValue().comment() + "\n");
                w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
            }
        }
    }
}
