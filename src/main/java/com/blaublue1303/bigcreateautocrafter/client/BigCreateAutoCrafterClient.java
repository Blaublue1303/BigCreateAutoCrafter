package com.blaublue1303.bigcreateautocrafter.client;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.client.smoketest.ClientSmokeTest;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point (never loaded on a dedicated server). */
@Mod(value = BigCreateAutoCrafter.MODID, dist = Dist.CLIENT)
public class BigCreateAutoCrafterClient {
    public BigCreateAutoCrafterClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(BcacKeyMappings::register);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientProfileCache.clear());
        NeoForge.EVENT_BUS.addListener(CrafterClickHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ProfileSelector::onInit);
        NeoForge.EVENT_BUS.addListener(ProfileSelector::onRender);
        NeoForge.EVENT_BUS.addListener(ProfileSelector::onMouseClicked);
        NeoForge.EVENT_BUS.addListener(ProfileSelector::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(BcacKeyMappings::onClientTick);
        if (ClientSmokeTest.enabled()) {
            NeoForge.EVENT_BUS.addListener(ClientSmokeTest::onClientTick);
            NeoForge.EVENT_BUS.addListener(ClientSmokeTest::onScreenOpening);
            NeoForge.EVENT_BUS.addListener(ClientSmokeTest::onScreenClosing);
        }
    }
}
