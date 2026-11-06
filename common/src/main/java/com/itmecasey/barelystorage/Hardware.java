package com.itmecasey.barelystorage;

/** The computing power a network's racks offer: threads, RAM and how deep a crafting chain may go. */
public record Hardware(int basicCores, int ramBlades, int quantumCores, int threads, int ram, int maxDepth) {
    public static final Hardware NONE = of(0, 0, 0);

    public static Hardware of(int basicCores, int ramBlades, int quantumCores) {
        int cores = basicCores + quantumCores;
        int threads = basicCores * Settings.getInt("hardware.basic_core.threads")
                + ramBlades * Settings.getInt("hardware.ram_blade.threads")
                + quantumCores * Settings.getInt("hardware.quantum_core.threads");
        int ram = basicCores * Settings.getInt("hardware.basic_core.ram")
                + ramBlades * Settings.getInt("hardware.ram_blade.ram")
                + quantumCores * Settings.getInt("hardware.quantum_core.ram");
        int depth = quantumCores > 0 ? Integer.MAX_VALUE
                : ramBlades > 0 ? Settings.getInt("hardware.ram_blade.depth") : Settings.getInt("hardware.basic.depth");
        if (cores == 0) {
            threads = 0;
            ram = 0;
            depth = 0;
        }
        return new Hardware(basicCores, ramBlades, quantumCores, threads, ram, depth);
    }

    public int cores() {
        return basicCores + quantumCores;
    }

    public Hardware plus(Hardware o) {
        return of(basicCores + o.basicCores, ramBlades + o.ramBlades, quantumCores + o.quantumCores);
    }
}
