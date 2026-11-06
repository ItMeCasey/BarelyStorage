package com.itmecasey.barelystorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageTest {
    static final ItemKey IRON = ItemKey.of("minecraft:iron_ingot");
    static final ItemKey GOLD = ItemKey.of("minecraft:gold_ingot");
    static final ItemKey COAL = ItemKey.of("minecraft:coal");

    @BeforeEach
    void reset() {
        Settings.reset();
    }

    /** A cartridge-backed provider with a priority, like a rack drive or a Storage Bus. */
    static final class Prov implements StorageProvider {
        final Cartridge c;
        final Priority p;

        Prov(Tier t, Priority p) {
            this.c = new Cartridge(t);
            this.p = p;
        }

        public Priority priority() {
            return p;
        }

        public void collect(Map<ItemKey, Long> out) {
            c.view().forEach((k, v) -> out.merge(k, v, Long::sum));
        }

        public long count(ItemKey key) {
            return c.count(key);
        }

        public long insert(ItemKey key, long amount, boolean simulate) {
            return c.insert(key, amount, simulate);
        }

        public long extract(ItemKey key, long amount, boolean simulate) {
            return c.extract(key, amount, simulate);
        }
    }

    @Test
    void cartridgeStopsAtCapacity() {
        Settings.set("tier.iron.capacity", 100);
        Cartridge c = new Cartridge(Tier.IRON);
        assertEquals(60, c.insert(IRON, 60, false));
        assertEquals(40, c.insert(GOLD, 70, false));
        assertEquals(100, c.used());
        assertEquals(0, c.insert(IRON, 1, false));
        assertEquals(2, c.types());
    }

    @Test
    void cartridgeLimitsDistinctTypes() {
        Settings.set("tier.iron.types", 2);
        Cartridge c = new Cartridge(Tier.IRON);
        c.insert(IRON, 1, false);
        c.insert(GOLD, 1, false);
        assertEquals(0, c.insert(COAL, 5, false), "a third type does not fit");
        assertEquals(5, c.insert(IRON, 5, false), "an existing type still fits");
    }

    @Test
    void simulateChangesNothingAndExtractFreesTypes() {
        Cartridge c = new Cartridge(Tier.STEEL);
        assertEquals(10, c.insert(IRON, 10, true));
        assertEquals(0, c.used());
        c.insert(IRON, 10, false);
        assertEquals(4, c.extract(IRON, 4, false));
        assertEquals(6, c.extract(IRON, 99, false));
        assertEquals(0, c.types());
        assertEquals(0, c.used());
    }

    @Test
    void tiersGrowWithCapacity() {
        long last = 0;
        for (Tier t : Tier.values()) {
            assertTrue(t.capacity() > last);
            last = t.capacity();
        }
    }

    @Test
    void poolInsertsByPriority() {
        StoragePool pool = new StoragePool();
        Prov low = new Prov(Tier.IRON, Priority.LOW);
        Prov high = new Prov(Tier.IRON, Priority.HIGH);
        Prov normal = new Prov(Tier.IRON, Priority.NORMAL);
        pool.add(low);
        pool.add(normal);
        pool.add(high);
        assertEquals(50, pool.insert(IRON, 50, false));
        assertEquals(50, high.c.count(IRON));
        assertEquals(0, normal.c.count(IRON));
        assertEquals(0, low.c.count(IRON));
    }

    @Test
    void poolOverflowsToNextPriorityAndExtractsLowFirst() {
        Settings.set("tier.iron.capacity", 10);
        StoragePool pool = new StoragePool();
        Prov high = new Prov(Tier.IRON, Priority.HIGH);
        Prov low = new Prov(Tier.IRON, Priority.LOW);
        pool.add(high);
        pool.add(low);
        assertEquals(15, pool.insert(IRON, 15, false));
        assertEquals(10, high.c.count(IRON));
        assertEquals(5, low.c.count(IRON));
        assertEquals(7, pool.extract(IRON, 7, false));
        assertEquals(0, low.c.count(IRON));
        assertEquals(8, high.c.count(IRON));
        assertEquals(8, pool.count(IRON));
    }

    @Test
    void poolPrefersProviderThatAlreadyHoldsTheItem() {
        StoragePool pool = new StoragePool();
        Prov a = new Prov(Tier.IRON, Priority.NORMAL);
        Prov b = new Prov(Tier.IRON, Priority.NORMAL);
        b.c.insert(IRON, 1, false);
        pool.add(a);
        pool.add(b);
        pool.insert(IRON, 5, false);
        assertEquals(6, b.c.count(IRON));
        assertEquals(0, a.c.count(IRON));
    }

    @Test
    void viewMergesProviders() {
        StoragePool pool = new StoragePool();
        Prov a = new Prov(Tier.IRON, Priority.NORMAL);
        Prov b = new Prov(Tier.IRON, Priority.LOW);
        a.c.insert(IRON, 3, false);
        b.c.insert(IRON, 4, false);
        b.c.insert(GOLD, 1, false);
        pool.add(a);
        pool.add(b);
        Map<ItemKey, Long> view = pool.view();
        assertEquals(7L, view.get(IRON));
        assertEquals(1L, view.get(GOLD));
    }

    @Test
    void cartridgeReloadKeepsContentsAndDropsOverflow() {
        Cartridge c = new Cartridge(Tier.IRON);
        c.insert(IRON, 500, false);
        c.insert(GOLD, 20, false);
        Map<ItemKey, Long> saved = new LinkedHashMap<>(c.view());
        Cartridge d = new Cartridge(Tier.IRON);
        d.load(saved);
        assertEquals(500, d.count(IRON));
        assertEquals(520, d.used());
        Settings.set("tier.iron.capacity", 100);
        Cartridge e = new Cartridge(Tier.IRON);
        e.load(saved);
        assertTrue(e.used() <= 100);
    }

    @Test
    void itemKeyRoundTrips() {
        ItemKey k = new ItemKey("minecraft:diamond_pickaxe", "{Damage:3}");
        assertEquals(k, ItemKey.decode(k.encode()));
        assertEquals(IRON, ItemKey.decode(IRON.encode()));
    }

    @Test
    void graphFindsConnectedComponent() {
        // 1-2-3 and 10-11 are separate networks.
        java.util.function.LongFunction<long[]> n = id -> switch ((int) id) {
            case 1 -> new long[] {2};
            case 2 -> new long[] {1, 3};
            case 3 -> new long[] {2};
            case 10 -> new long[] {11};
            case 11 -> new long[] {10};
            default -> new long[0];
        };
        assertEquals(Set.of(1L, 2L, 3L), NetGraph.component(1, n, 100));
        assertEquals(Set.of(10L, 11L), NetGraph.component(11, n, 100));
        assertEquals(2, NetGraph.component(1, n, 2).size());
    }

    @Test
    void powerModelPaysOrGoesOffline() {
        int cost = PowerModel.perTick(2, 1, 0, 0);
        assertEquals(8 + 4 + 4, cost);
        assertEquals(100 - cost, PowerModel.pay(100, cost));
        assertEquals(-1, PowerModel.pay(cost - 1, cost));
        assertEquals(8 + 12, PowerModel.perTick(0, 0, 0, 1));
    }
}
