package com.itmecasey.barelystorage.neoforge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.client.GridScreen;
import com.itmecasey.barelystorage.client.PatternScreen;
import com.itmecasey.barelystorage.client.RackScreen;
import com.itmecasey.barelystorage.client.TerminalScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only setup: the screens. Never loaded on a dedicated server. */
@EventBusSubscriber(modid = BarelyStorage.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ModContent.RACK_MENU, RackScreen::new);
        event.register(ModContent.TERMINAL_MENU, TerminalScreen::new);
        event.register(ModContent.PATTERN_MENU, PatternScreen::new);
        event.register(ModContent.INTERFACE_MENU, GridScreen::new);
        event.register(ModContent.ASSEMBLER_MENU, GridScreen::new);
    }
}
