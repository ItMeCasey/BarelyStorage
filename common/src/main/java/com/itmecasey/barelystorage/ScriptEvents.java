package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Listeners and filters that ModJS scripts register. Minecraft-free: the game code fires events with plain data, and a script that
 * throws is logged on one line and skipped, so a script can never break the network.
 */
public final class ScriptEvents {
    private static final Logger LOG = Logger.getLogger("barelystorage");

    /** The events scripts can listen to. */
    public static final List<String> TYPES = List.of("rack_online", "rack_offline", "craft_queued", "craft_failed", "craft_halted",
            "pattern_encoded", "items_taken");

    /** What a script receives: the event name and its values by key. */
    public static final class Event {
        private final String type;
        private final Map<String, Object> data;

        Event(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }

        public String type() {
            return type;
        }

        public Object get(String key) {
            return data.get(key);
        }

        public String string(String key) {
            Object v = data.get(key);
            return v == null ? "" : String.valueOf(v);
        }

        public double number(String key) {
            Object v = data.get(key);
            return v instanceof Number n ? n.doubleValue() : 0;
        }

        public List<String> keys() {
            return new ArrayList<>(data.keySet());
        }

        @Override
        public String toString() {
            return type + data;
        }
    }

    private static final Map<String, List<Consumer<Event>>> LISTENERS = new ConcurrentHashMap<>();
    private static final List<Predicate<String>> INSERT_FILTERS = new CopyOnWriteArrayList<>();

    private ScriptEvents() {
    }

    public static void on(String type, Consumer<Event> listener) {
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown BarelyStorage event " + type + ". Events: " + TYPES);
        }
        LISTENERS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static boolean hasListeners(String type) {
        List<Consumer<Event>> l = LISTENERS.get(type);
        return l != null && !l.isEmpty();
    }

    /** Tells every listener; nothing is built when nobody listens. Arguments are key, value, key, value... */
    public static void fire(String type, Object... keyValues) {
        List<Consumer<Event>> listeners = LISTENERS.get(type);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            data.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        Event event = new Event(type, data);
        for (Consumer<Event> l : listeners) {
            try {
                l.accept(event);
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("A script listener for " + type + " failed: " + e);
            }
        }
    }

    /** Scripts can refuse items: a filter returning false keeps that item out of every network. */
    public static void addInsertFilter(Predicate<String> filter) {
        INSERT_FILTERS.add(filter);
    }

    public static boolean allowInsert(String itemId) {
        for (Predicate<String> f : INSERT_FILTERS) {
            try {
                if (!f.test(itemId)) {
                    return false;
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("A script insert filter failed: " + e);
            }
        }
        return true;
    }

    /** Removes every listener and filter (tests, script reload). */
    public static void clear() {
        LISTENERS.clear();
        INSERT_FILTERS.clear();
    }

    public static Set<String> listened() {
        return LISTENERS.keySet();
    }
}
