package com.itmecasey.barelystorage;

/** What a Server Rack costs to run, in FE per tick. */
public final class PowerModel {
    private PowerModel() {
    }

    public static int perTick(int cartridges, int basicCores, int ramBlades, int quantumCores) {
        int blade = Settings.getInt("power.per_blade");
        return Settings.getInt("power.base_per_tick")
                + cartridges * Settings.getInt("power.per_cartridge")
                + (basicCores + ramBlades) * blade
                + quantumCores * blade * 3;
    }

    /** Pays one tick of upkeep out of {@code stored}; returns the new stored amount, or -1 when the rack could not pay (it is offline this tick). */
    public static int pay(int stored, int cost) {
        return stored >= cost ? stored - cost : -1;
    }
}
