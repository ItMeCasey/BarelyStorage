package com.itmecasey.barelystorage.neoforge.dev;

import com.itmecasey.barelystorage.dev.SelfTest;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Development hooks for NeoForge: the server self test. Only built with -PdevTools. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install(IEventBus modBus) {
        if (SelfTest.requested()) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
    }
}
