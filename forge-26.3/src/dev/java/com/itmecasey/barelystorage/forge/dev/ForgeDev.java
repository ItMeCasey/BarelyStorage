package com.itmecasey.barelystorage.forge.dev;

import com.itmecasey.barelystorage.dev.SelfTest;
import net.minecraftforge.event.server.ServerStartedEvent;

/** Development hooks for Forge: the server self test. Only built with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install(Object context) {
        if (SelfTest.requested()) {
            ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
    }
}
