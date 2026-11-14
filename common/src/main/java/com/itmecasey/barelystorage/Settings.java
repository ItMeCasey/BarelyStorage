package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every tunable number in the mod, in one list of keys, defaults, ranges and descriptions. The loaders build their config
 * spec ({@code barelystorage-common.toml}) from {@link #defs()} and push the loaded values back with {@link #set}, so the
 * game code and the tests read the same {@code Settings.getInt(...)} calls on every loader.
 */
public final class Settings {
    /** One setting: a whole number between {@code min} and {@code max} with a description for the config file. */
    public record Def(String key, int fallback, int min, int max, String comment) {
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, int fallback, int min, int max, String comment) {
        DEFS.put(key, new Def(key, fallback, min, max, comment));
    }

    static {
        def("power.buffer", 400000, 1, Integer.MAX_VALUE, "Energy (FE) a Server Rack can hold.");
        def("power.max_receive", 4000, 1, Integer.MAX_VALUE, "Most FE a Server Rack takes in per tick.");
        def("power.base_per_tick", 8, 0, 1000000, "FE per tick a powered rack always uses.");
        def("power.per_cartridge", 2, 0, 1000000, "Extra FE per tick for each cartridge in a rack.");
        def("power.per_blade", 4, 0, 1000000, "Extra FE per tick for each blade in a rack (a Quantum Core costs three times this).");
        def("power.efficiency_percent", 10, 0, 100, "Percent of a rack's upkeep each Efficiency card in the network takes off.");
        def("power.efficiency_max_percent", 50, 0, 100, "Most upkeep, in percent, Efficiency cards can take off.");

        def("tier.iron.capacity", 8192, 1, Integer.MAX_VALUE, "Items an Iron Drive holds.");
        def("tier.steel.capacity", 65536, 1, Integer.MAX_VALUE, "Items a Steel Drive holds.");
        def("tier.gold.capacity", 262144, 1, Integer.MAX_VALUE, "Items a Gold Drive holds.");
        def("tier.obsidian.capacity", 4194304, 1, Integer.MAX_VALUE, "Items an Obsidian Drive holds.");
        def("tier.quantum.capacity", 134217728, 1, Integer.MAX_VALUE, "Items a Quantum Drive holds.");
        def("tier.iron.types", 16, 1, Integer.MAX_VALUE, "Different item types an Iron Drive holds.");
        def("tier.steel.types", 32, 1, Integer.MAX_VALUE, "Different item types a Steel Drive holds.");
        def("tier.gold.types", 64, 1, Integer.MAX_VALUE, "Different item types a Gold Drive holds.");
        def("tier.obsidian.types", 128, 1, Integer.MAX_VALUE, "Different item types an Obsidian Drive holds.");
        def("tier.quantum.types", 512, 1, Integer.MAX_VALUE, "Different item types a Quantum Drive holds.");

        def("rack.drive_bays", 8, 4, 8, "Drive bays in a Server Rack (4 to 8).");
        def("rack.blade_slots", 4, 1, 4, "Blade slots in a Server Rack.");

        def("hardware.basic_core.threads", 1, 0, 1000000, "Threads one Basic Core adds.");
        def("hardware.basic_core.ram", 8, 0, 1000000, "RAM units one Basic Core adds.");
        def("hardware.ram_blade.threads", 4, 0, 1000000, "Threads one Multi-Threaded RAM Blade adds.");
        def("hardware.ram_blade.ram", 32, 0, 1000000, "RAM units one Multi-Threaded RAM Blade adds.");
        def("hardware.quantum_core.threads", 8, 0, 1000000, "Threads one Quantum Core adds.");
        def("hardware.quantum_core.ram", 128, 0, 1000000, "RAM units one Quantum Core adds.");
        def("hardware.ram_blade.depth", 3, 1, 1000, "Deepest chain of patterns a rack with a RAM Blade but no Quantum Core can plan.");
        def("hardware.basic.depth", 1, 1, 1000, "Deepest chain of patterns a rack with only Basic Cores can plan.");

        def("crafting.assembler_ticks_per_run", 10, 1, Integer.MAX_VALUE, "Ticks an Assembler needs for one run of a crafting pattern.");
        def("crafting.max_batch", 16, 1, Integer.MAX_VALUE, "Most runs of a pattern started at once.");
        def("crafting.job_limit", 16, 1, Integer.MAX_VALUE, "Most crafting jobs a network queues.");

        def("nodes.interval_ticks", 8, 1, Integer.MAX_VALUE, "Ticks between item moves of an Interface Node, Import Node or Storage Bus.");
        def("nodes.items_per_move", 32, 1, Integer.MAX_VALUE, "Most items an Interface or Import Node moves per step.");
    }

    private Settings() {
    }

    /** Every setting in file order. */
    public static List<Def> defs() {
        return new ArrayList<>(DEFS.values());
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Def e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, String.valueOf(e.fallback()));
    }

    /** Every setting key, sorted. */
    public static List<String> keys() {
        return new ArrayList<>(new java.util.TreeSet<>(DEFS.keySet()));
    }

    /** The current value of a setting as text. */
    public static String value(String key) {
        return raw(key);
    }

    /** Sets one value: the loaders call this when the config file loads or reloads; tests and scripts use it too. Not saved to the file. */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return DEFS.get(key).fallback();
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }
}
