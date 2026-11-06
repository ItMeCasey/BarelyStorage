package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The result of planning one crafting request: ordered steps and what they cost in hardware. */
public final class CraftPlan {
    public enum Error {
        NONE, NO_PATTERN, MISSING, CYCLE, NO_CORE, THREAD_EXHAUSTION, OUT_OF_MEMORY
    }

    /** A pattern that must run {@code runs} times; {@code height} is 1 for patterns whose inputs all come from stock. */
    public static final class Step {
        public final Pattern pattern;
        public int runs;
        public int height;
        /** Indexes (in the plan's step list) of the steps whose products this step consumes. */
        public final List<Integer> dependsOn = new ArrayList<>();

        Step(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    public final ItemKey target;
    public final long amount;
    public final List<Step> steps = new ArrayList<>();
    /** What the plan takes from stock. */
    public final Map<ItemKey, Long> fromStock = new LinkedHashMap<>();
    /** Items that are needed but neither in stock nor craftable. */
    public final Map<ItemKey, Long> missing = new LinkedHashMap<>();
    public Error error = Error.NONE;
    public String message = "";
    public int depth;
    public int parallelism;
    public int ram;

    CraftPlan(ItemKey target, long amount) {
        this.target = target;
        this.amount = amount;
    }

    public boolean ok() {
        return error == Error.NONE;
    }

    /** Checks the plan against the hardware; sets {@link #error} and {@link #message} and returns true when it fits. */
    public boolean fits(Hardware hw) {
        if (error == Error.NO_PATTERN || error == Error.MISSING || error == Error.CYCLE) {
            return false;
        }
        error = Error.NONE;
        message = "";
        if (hw.cores() == 0) {
            error = Error.NO_CORE;
            message = "No Core installed: a rack needs a Basic Core or Quantum Core before it can run crafting";
            return false;
        }
        if (parallelism > hw.threads() || depth > hw.maxDepth()) {
            error = Error.THREAD_EXHAUSTION;
            message = "Thread Exhaustion: job needs " + parallelism + " threads at chain depth " + depth
                    + ", hardware has " + hw.threads() + " threads and allows depth "
                    + (hw.maxDepth() == Integer.MAX_VALUE ? "unlimited" : String.valueOf(hw.maxDepth()))
                    + ". Install more CPU or RAM blades";
            return false;
        }
        if (ram > hw.ram()) {
            error = Error.OUT_OF_MEMORY;
            message = "Out of Memory: job needs " + ram + " RAM, hardware has " + hw.ram() + ". Install more RAM blades";
            return false;
        }
        return true;
    }
}
