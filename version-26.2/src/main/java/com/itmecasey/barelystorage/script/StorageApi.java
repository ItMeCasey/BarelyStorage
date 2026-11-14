package com.itmecasey.barelystorage.script;

import com.itmecasey.barelystorage.ScriptEvents;
import com.itmecasey.barelystorage.Settings;
import com.itmecasey.barelystorage.StorageNetwork;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The BarelyStorage global of ModJS server scripts: events, read access to a network, and a few settings and item rules.
 * Everything uses strings, numbers and plain lists so a script engine can call it.
 */
public final class StorageApi {
    /** The events {@link #on} accepts. */
    public List<String> events() {
        return ScriptEvents.TYPES;
    }

    /** Listens to an event (see events()); the function gets the event with get(key), string(key) and number(key). */
    public void on(String event, Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on(event, listener);
    }

    /** Refuses items: when the function returns false for an item id, no network takes that item. */
    public void addInsertFilter(Predicate<String> allow) {
        ScriptEvents.addInsertFilter(allow);
    }

    /** The network of the rack, cable, node or terminal at a position, or null when there is none (or the server is not running yet). */
    public NetworkView network(String dimension, int x, int y, int z) {
        ServerLevel level = ScriptHost.level(dimension);
        if (level == null) {
            return null;
        }
        StorageNetwork net = StorageNetwork.find(level, new BlockPos(x, y, z));
        return net.racks.isEmpty() ? null : new NetworkView(net);
    }

    public List<String> settingKeys() {
        return Settings.keys();
    }

    /** A setting as text, for example power.buffer. */
    public String setting(String key) {
        return Settings.value(key);
    }

    /** Changes a setting until the game closes (the config file is not touched). Unknown keys throw. */
    public void setSetting(String key, String value) {
        Settings.value(key);
        Settings.set(key, value);
    }
}
