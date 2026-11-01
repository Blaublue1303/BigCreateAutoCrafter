package com.blaublue1303.bigcreateautocrafter;

import org.slf4j.Logger;

import com.blaublue1303.bigcreateautocrafter.net.BcacNetwork;
import com.blaublue1303.bigcreateautocrafter.profile.BcacCommands;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileSync;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Common entry point: config, network, profile editing (Ctrl + right-click, /bcac) and sync. */
@Mod(BigCreateAutoCrafter.MODID)
public class BigCreateAutoCrafter {
    public static final String MODID = "bigcreateautocrafter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BigCreateAutoCrafter(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(BcacNetwork::register);
        NeoForge.EVENT_BUS.addListener(ProfileSync::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ProfileService::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(ProfileService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(BcacCommands::onRegisterCommands);
    }
}
