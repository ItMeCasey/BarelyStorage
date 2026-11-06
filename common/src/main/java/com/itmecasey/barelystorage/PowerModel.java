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

    /** Upkeep after Efficiency cards: each card in the network takes off a share, up to a cap, and a rack always pays at least 1 FE per tick. */
    public static int withEfficiency(int cost, int cards) {
        int percent = Math.min(Settings.getInt("power.efficiency_max_percent"), Math.max(0, cards) * Settings.getInt("power.efficiency_percent"));
        return Math.max(1, cost - cost * percent / 100);
    }

    /** Pays one tick of upkeep out of {@code stored}; returns the new stored amount, or -1 when the rack could not pay (it is offline this tick). */
    public static int pay(int stored, int cost) {
        return stored >= cost ? stored - cost : -1;
    }
}
