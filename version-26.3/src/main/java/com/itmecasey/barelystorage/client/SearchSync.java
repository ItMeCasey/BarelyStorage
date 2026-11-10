package com.itmecasey.barelystorage.client;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * The search box of the installed recipe viewer, registered by the compat plugins. The terminal can mirror it both ways. When
 * several viewers register (BEI also runs JEI plugins through its bridge), the one with the highest priority wins: BEI, then
 * REI, then JEI.
 */
public final class SearchSync {
    public static final int JEI = 1;
    public static final int REI = 2;
    public static final int BEI = 3;

    private record Source(Supplier<String> get, Consumer<String> set) {
    }

    private static final Map<Integer, Source> SOURCES = new TreeMap<>();

    private SearchSync() {
    }

    /** Registers (or, with nulls, removes) the search box of one viewer. */
    public static synchronized void register(int priority, @Nullable Supplier<String> get, @Nullable Consumer<String> set) {
        if (get == null || set == null) {
            SOURCES.remove(priority);
        } else {
            SOURCES.put(priority, new Source(get, set));
        }
    }

    private static synchronized Source best() {
        Source best = null;
        for (Source s : SOURCES.values()) {
            best = s;
        }
        return best;
    }

    public static boolean available() {
        return best() != null;
    }

    public static String get() {
        try {
            Source s = best();
            String text = s == null ? "" : s.get().get();
            return text == null ? "" : text;
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static void set(String text) {
        try {
            Source s = best();
            if (s != null) {
                s.set().accept(text);
            }
        } catch (RuntimeException ignored) {
            // The viewer may not be ready; the next tick tries again.
        }
    }
}
