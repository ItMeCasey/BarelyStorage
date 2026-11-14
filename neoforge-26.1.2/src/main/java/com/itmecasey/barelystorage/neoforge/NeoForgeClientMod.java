package com.itmecasey.barelystorage.neoforge;

import com.itmecasey.barelystorage.BarelyStorage;
import com.itmecasey.barelystorage.client.ClientSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point: connects the screens' choices to the client config and adds the in-game config screen. */
@Mod(value = BarelyStorage.MODID, dist = Dist.CLIENT)
public final class NeoForgeClientMod {
    public NeoForgeClientMod(ModContainer container) {
        ClientSettings.sink = NeoForgeConfig::storeClient;
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
