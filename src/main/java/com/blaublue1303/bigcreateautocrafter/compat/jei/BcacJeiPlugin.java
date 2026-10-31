package com.blaublue1303.bigcreateautocrafter.compat.jei;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;

import net.minecraft.resources.ResourceLocation;

/**
 * JEI entry point (only loaded by JEI, so JEI stays an optional dependency).
 * Deliberately registers nothing yet: Create's own {@code StockKeeperTransferHandler} keeps handling the "+"
 * button until our transfer handler is added (docs/06-todo.md step 9, after test T2 on handler priority).
 */
@JeiPlugin
public class BcacJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
}
