package com.itmecasey.barelystorage;

import java.util.LinkedHashMap;
import java.util.Map;

/** The contents of one storage cartridge: item counts under a total capacity and a limit on distinct item types. */
public final class Cartridge {
    private final Tier tier;
    private final Map<ItemKey, Long> contents = new LinkedHashMap<>();
    private long used;

    public Cartridge(Tier tier) {
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    public long used() {
        return used;
    }

    public long capacity() {
        return tier.capacity();
    }

    public int types() {
        return contents.size();
    }

    public long count(ItemKey key) {
        return contents.getOrDefault(key, 0L);
    }

    public Map<ItemKey, Long> view() {
        return java.util.Collections.unmodifiableMap(contents);
    }

    /** Stores up to {@code amount} items and returns how many fit. */
    public long insert(ItemKey key, long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        if (!contents.containsKey(key) && contents.size() >= tier.maxTypes()) {
            return 0;
        }
        long accepted = Math.min(amount, Math.max(0, capacity() - used));
        if (accepted > 0 && !simulate) {
            contents.merge(key, accepted, Long::sum);
            used += accepted;
        }
        return accepted;
    }

    /** Removes up to {@code amount} items and returns how many came out. */
    public long extract(ItemKey key, long amount, boolean simulate) {
        long have = count(key);
        long taken = Math.min(have, Math.max(0, amount));
        if (taken > 0 && !simulate) {
            if (taken == have) {
                contents.remove(key);
            } else {
                contents.put(key, have - taken);
            }
            used -= taken;
        }
        return taken;
    }

    /** Replaces the contents (used when loading); entries past the limits are dropped, never kept over capacity. */
    public void load(Map<ItemKey, Long> entries) {
        contents.clear();
        used = 0;
        for (Map.Entry<ItemKey, Long> e : entries.entrySet()) {
            insert(e.getKey(), e.getValue(), false);
        }
    }
}
