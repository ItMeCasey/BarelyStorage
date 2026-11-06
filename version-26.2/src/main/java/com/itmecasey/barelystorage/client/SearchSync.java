package com.itmecasey.barelystorage.client;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/** The search box of the installed recipe viewer (JEI, REI), registered by the compat plugins. The terminal can mirror it both ways. */
public final class SearchSync {
    @Nullable
    private static Supplier<String> getter;
    @Nullable
    private static Consumer<String> setter;

    private SearchSync() {
    }

    public static void register(@Nullable Supplier<String> get, @Nullable Consumer<String> set) {
        getter = get;
        setter = set;
    }

    public static boolean available() {
        return getter != null && setter != null;
    }

    public static String get() {
        try {
            String s = getter == null ? "" : getter.get();
            return s == null ? "" : s;
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static void set(String text) {
        try {
            if (setter != null) {
                setter.accept(text);
            }
        } catch (RuntimeException ignored) {
            // The viewer may not be ready; the next tick tries again.
        }
    }
}
