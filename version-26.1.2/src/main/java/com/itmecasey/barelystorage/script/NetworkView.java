package com.itmecasey.barelystorage.script;

import com.itmecasey.barelystorage.Hardware;
import com.itmecasey.barelystorage.ItemKey;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;

/** A read-only snapshot of one network, taken when a script asks for it. */
public final class NetworkView {
    private final int racks;
    private final boolean online;
    private final long energy;
    private final long maxEnergy;
    private final int upkeep;
    private final int threads;
    private final int ram;
    private final int depth;
    private final long used;
    private final long capacity;
    private final int jobs;
    private final String error;
    private final Map<String, Long> items = new LinkedHashMap<>();

    NetworkView(StorageNetwork net) {
        this.racks = net.racks.size();
        this.online = net.online();
        long e = 0;
        long max = 0;
        int cost = 0;
        long u = 0;
        long c = 0;
        for (RackBlockEntity r : net.racks) {
            e += r.energy();
            max += r.maxEnergy();
            cost += r.upkeep();
            u += r.used();
            c += r.capacity();
        }
        RackBlockEntity leader = net.leader();
        this.jobs = leader == null ? 0 : leader.scheduler().jobs().size();
        this.error = leader == null ? "" : leader.scheduler().lastError();
        Hardware hw = net.hardware();
        this.energy = e;
        this.maxEnergy = max;
        this.upkeep = cost;
        this.threads = hw.threads();
        this.ram = hw.ram();
        this.depth = hw.maxDepth() == Integer.MAX_VALUE ? -1 : hw.maxDepth();
        this.used = u;
        this.capacity = c;
        if (online) {
            for (Map.Entry<ItemKey, Long> en : net.pool().view().entrySet()) {
                items.merge(en.getKey().id(), en.getValue(), Long::sum);
            }
        }
    }

    public int racks() {
        return racks;
    }

    public boolean online() {
        return online;
    }

    public long energy() {
        return energy;
    }

    public long maxEnergy() {
        return maxEnergy;
    }

    public int upkeep() {
        return upkeep;
    }

    public int threads() {
        return threads;
    }

    public int ram() {
        return ram;
    }

    /** The deepest pattern chain the network can plan; -1 means no limit (a Quantum Core). */
    public int depth() {
        return depth;
    }

    public long used() {
        return used;
    }

    public long capacity() {
        return capacity;
    }

    public int jobs() {
        return jobs;
    }

    public String lastError() {
        return error;
    }

    public int itemTypes() {
        return items.size();
    }

    /** How many of an item the network holds (item id such as minecraft:cobblestone). */
    public long count(String itemId) {
        return items.getOrDefault(itemId, 0L);
    }

    /** Item id to stored amount, for every item type in the network. */
    public Map<String, Long> items() {
        return new LinkedHashMap<>(items);
    }
}
