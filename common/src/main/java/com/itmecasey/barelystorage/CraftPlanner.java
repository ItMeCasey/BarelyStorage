package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Works out which patterns must run, how often, and what that costs in threads, chain depth and RAM. */
public final class CraftPlanner {
    private final List<Pattern> patterns;
    private final Map<ItemKey, Long> stock;
    private final Map<Pattern, Integer> stepIndex = new HashMap<>();
    private final Set<ItemKey> touched = new HashSet<>();
    private CraftPlan plan;

    private CraftPlanner(List<Pattern> patterns, Map<ItemKey, Long> stock) {
        this.patterns = patterns;
        this.stock = new HashMap<>(stock);
    }

    /** Plans {@code amount} of {@code target} from the given stock and patterns. The hardware is not consulted here; see {@link CraftPlan#fits}. */
    public static CraftPlan plan(ItemKey target, long amount, Map<ItemKey, Long> stock, List<Pattern> patterns) {
        CraftPlanner p = new CraftPlanner(patterns, stock);
        p.plan = new CraftPlan(target, amount);
        if (p.firstPattern(target) == null) {
            p.plan.error = CraftPlan.Error.NO_PATTERN;
            p.plan.message = "No pattern makes " + target.id();
            return p.plan;
        }
        int h = p.need(target, amount, new ArrayList<>(), true, new java.util.TreeSet<>());
        if (p.plan.error != CraftPlan.Error.NONE) {
            return p.plan;
        }
        p.plan.depth = h;
        Map<Integer, Integer> perLevel = new HashMap<>();
        int par = 0;
        for (CraftPlan.Step s : p.plan.steps) {
            par = Math.max(par, perLevel.merge(s.height, 1, Integer::sum));
        }
        p.plan.parallelism = par;
        p.plan.ram = p.plan.steps.size() * 3 + p.touched.size();
        return p.plan;
    }

    private Pattern firstPattern(ItemKey key) {
        for (Pattern pt : patterns) {
            if (pt.makes(key)) {
                return pt;
            }
        }
        return null;
    }

    /** @return the height of the step chain that produces the needed items (0 when stock covers it). */
    private int need(ItemKey key, long amount, List<ItemKey> chain, boolean forceCraft, Set<Integer> madeBy) {
        touched.add(key);
        long left = amount;
        if (!forceCraft) {
            long have = stock.getOrDefault(key, 0L);
            long take = Math.min(have, left);
            if (take > 0) {
                stock.put(key, have - take);
                plan.fromStock.merge(key, take, Long::sum);
                left -= take;
            }
            if (left <= 0) {
                return 0;
            }
        }
        Pattern pattern = firstPattern(key);
        if (pattern == null) {
            plan.missing.merge(key, left, Long::sum);
            plan.error = CraftPlan.Error.MISSING;
            plan.message = "Missing " + left + " x " + key.id() + " and no pattern makes it";
            return 0;
        }
        if (chain.contains(key)) {
            plan.error = CraftPlan.Error.CYCLE;
            plan.message = "Patterns for " + key.id() + " loop back on themselves";
            return 0;
        }
        int perRun = pattern.outputCount(key);
        long runs = (left + perRun - 1) / perRun;
        long surplus = runs * perRun - left;
        if (surplus > 0) {
            stock.merge(key, surplus, Long::sum);
        }
        // Other outputs of the pattern also land in stock.
        for (Pattern.Stack out : pattern.outputs()) {
            if (!out.key().equals(key)) {
                stock.merge(out.key(), out.count() * runs, Long::sum);
            }
        }
        chain.add(key);
        int childMax = 0;
        Set<Integer> deps = new java.util.TreeSet<>();
        for (Pattern.Stack in : pattern.inputs()) {
            int h = need(in.key(), in.count() * runs, chain, false, deps);
            childMax = Math.max(childMax, h);
            if (plan.error != CraftPlan.Error.NONE) {
                chain.remove(chain.size() - 1);
                return 0;
            }
        }
        chain.remove(chain.size() - 1);
        Integer existing = stepIndex.get(pattern);
        CraftPlan.Step step;
        int index;
        if (existing != null) {
            index = existing;
            step = plan.steps.get(index);
            step.runs += (int) runs;
        } else {
            step = new CraftPlan.Step(pattern);
            step.runs = (int) runs;
            index = plan.steps.size();
            plan.steps.add(step);
            stepIndex.put(pattern, index);
        }
        for (int d : deps) {
            if (d != index && !step.dependsOn.contains(d)) {
                step.dependsOn.add(d);
            }
        }
        step.height = Math.max(step.height, childMax + 1);
        madeBy.add(index);
        return step.height;
    }
}
