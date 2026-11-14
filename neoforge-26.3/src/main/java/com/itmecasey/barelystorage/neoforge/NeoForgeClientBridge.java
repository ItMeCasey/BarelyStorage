package com.itmecasey.barelystorage.neoforge;

import com.itmecasey.barelystorage.client.ClientSettings;

/** Hands the loaded client config to the screens. Only called when the client config loads, which only happens on a client. */
final class NeoForgeClientBridge {
    private NeoForgeClientBridge() {
    }

    static void apply(int[] v) {
        ClientSettings.apply(v[0], v[1], v[2], v[3], v[4]);
    }
}
