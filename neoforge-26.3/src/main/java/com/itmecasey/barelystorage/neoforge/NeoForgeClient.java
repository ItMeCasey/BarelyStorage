package com.itmecasey.barelystorage.neoforge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.client.NodeScreen;
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
        for (var type : ModContent.terminalMenus()) {
            event.register(type, TerminalScreen::new);
        }
        event.register(ModContent.PATTERN_MENU, PatternScreen::new);
        event.register(ModContent.INTERFACE_MENU, NodeScreen::new);
        event.register(ModContent.ASSEMBLER_MENU, NodeScreen::new);
        event.register(ModContent.BUS_MENU, NodeScreen::new);
        event.register(ModContent.IMPORT_MENU, NodeScreen::new);
        event.register(ModContent.EXPORT_MENU, NodeScreen::new);
    }
}
