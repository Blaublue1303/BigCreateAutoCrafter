package com.blaublue1303.bigcreateautocrafter;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per world, synced to clients). Limits are enforced by the server-side validation that is
 * added in docs/06-todo.md step 8; defaults follow docs/03-phase3-plan.md.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_REGION_WIDTH = BUILDER
            .comment("Maximum width (in crafters) of a single region.")
            .defineInRange("maxRegionWidth", 9, 1, 64);

    public static final ModConfigSpec.IntValue MAX_REGION_HEIGHT = BUILDER
            .comment("Maximum height (in crafters) of a single region.")
            .defineInRange("maxRegionHeight", 9, 1, 64);

    public static final ModConfigSpec.IntValue MAX_REGIONS_PER_PROFILE = BUILDER
            .comment("Maximum number of regions (crafter groups) per profile.")
            .defineInRange("maxRegionsPerProfile", 8, 1, 64);

    public static final ModConfigSpec.IntValue MAX_PROFILES_PER_PLAYER = BUILDER
            .comment("Maximum number of profiles a single player may own.")
            .defineInRange("maxProfilesPerPlayer", 16, 1, 256);

    public static final ModConfigSpec.BooleanValue AUTO_SELECT_SMALLEST_REGION = BUILDER
            .comment("If no region is chosen manually, use the smallest region of the profile the recipe fits into.")
            .define("autoSelectSmallestRegion", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
