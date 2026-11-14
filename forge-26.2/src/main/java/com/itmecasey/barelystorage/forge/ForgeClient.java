package com.itmecasey.barelystorage.forge;

import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.client.NodeScreen;
import com.itmecasey.barelystorage.client.QueueScreen;
import com.itmecasey.barelystorage.client.RackScreen;
import com.itmecasey.barelystorage.client.TerminalScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup: the screens. The event only fires on a client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install(FMLJavaModLoadingContext context) {
        com.itmecasey.barelystorage.client.ClientSettings.sink = ForgeConfig::storeClient;
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(ForgeClient::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModContent.RACK_MENU, RackScreen::new);
            for (var type : ModContent.terminalMenus()) {
                MenuScreens.register(type, TerminalScreen::new);
            }
            MenuScreens.register(ModContent.QUEUE_MENU, QueueScreen::new);
            MenuScreens.register(ModContent.INTERFACE_MENU, NodeScreen::new);
            MenuScreens.register(ModContent.ASSEMBLER_MENU, NodeScreen::new);
            MenuScreens.register(ModContent.BUS_MENU, NodeScreen::new);
            MenuScreens.register(ModContent.IMPORT_MENU, NodeScreen::new);
            MenuScreens.register(ModContent.EXPORT_MENU, NodeScreen::new);
        });
    }
}
