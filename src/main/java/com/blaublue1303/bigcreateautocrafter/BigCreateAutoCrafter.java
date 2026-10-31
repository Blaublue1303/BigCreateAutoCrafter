package com.blaublue1303.bigcreateautocrafter;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

/**
 * Phase 2 skeleton: intentionally contains no gameplay logic.
 * Real functionality is added only after the analysis in docs/ is verified against the modpack JARs.
 */
@Mod(BigCreateAutoCrafter.MODID)
public class BigCreateAutoCrafter {
    public static final String MODID = "bigcreateautocrafter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BigCreateAutoCrafter() {
        LOGGER.info("Big Create Auto Crafter skeleton loaded (no functionality yet)");
    }
}
