package com.itmecasey.barelystorage;

/** Storage cartridge tiers. Capacity and distinct-type limits come from the settings file. */
public enum Tier {
    IRON("iron"), STEEL("steel"), GOLD("gold"), OBSIDIAN("obsidian"), QUANTUM("quantum");

    private final String id;

    Tier(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public long capacity() {
        return (long) Settings.getDouble("tier." + id + ".capacity");
    }

    public int maxTypes() {
        return Settings.getInt("tier." + id + ".types");
    }

    public static Tier byId(String id) {
        for (Tier t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return IRON;
    }
}
