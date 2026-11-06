package com.itmecasey.barelystorage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongFunction;

/** Connected components over block positions packed into longs. The caller supplies each node's neighbors. */
public final class NetGraph {
    private NetGraph() {
    }

    /** Every node reachable from {@code start} (including it), in discovery order, stopping at {@code limit} nodes. */
    public static Set<Long> component(long start, LongFunction<long[]> neighbors, int limit) {
        Set<Long> seen = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < limit) {
            long at = queue.poll();
            for (long n : neighbors.apply(at)) {
                if (seen.size() < limit && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        return seen;
    }
}
