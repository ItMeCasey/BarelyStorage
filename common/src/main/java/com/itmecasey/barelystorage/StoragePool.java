package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The merged storage of a network: routes inserts by priority and answers what the network holds. */
public final class StoragePool {
    private final List<StorageProvider> providers = new ArrayList<>();

    public void add(StorageProvider p) {
        providers.add(p);
    }

    public int size() {
        return providers.size();
    }

    public Map<ItemKey, Long> view() {
        Map<ItemKey, Long> out = new TreeMap<>();
        for (StorageProvider p : providers) {
            p.collect(out);
        }
        return out;
    }

    public long count(ItemKey key) {
        long n = 0;
        for (StorageProvider p : providers) {
            n += p.count(key);
        }
        return n;
    }

    /** Inserts as much as fits: High before Normal before Low, and inside one priority the providers that already hold the item first. */
    public long insert(ItemKey key, long amount, boolean simulate) {
        List<StorageProvider> order = new ArrayList<>(providers);
        order.sort(Comparator.<StorageProvider>comparingInt(p -> p.priority().ordinal())
                .thenComparingInt(p -> p.count(key) > 0 ? 0 : 1));
        long left = amount;
        for (StorageProvider p : order) {
            if (left <= 0) {
                break;
            }
            left -= p.insert(key, left, simulate);
        }
        return amount - left;
    }

    /** Extracts from Low first so the high priority storage keeps its stock longest. */
    public long extract(ItemKey key, long amount, boolean simulate) {
        List<StorageProvider> order = new ArrayList<>(providers);
        order.sort(Comparator.<StorageProvider>comparingInt(p -> -p.priority().ordinal()));
        long left = amount;
        for (StorageProvider p : order) {
            if (left <= 0) {
                break;
            }
            left -= p.extract(key, left, simulate);
        }
        return amount - left;
    }
}
