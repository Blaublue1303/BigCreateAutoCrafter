package com.blaublue1303.bigcreateautocrafter.client;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.recipe.Placement;

import net.minecraft.world.item.crafting.Recipe;

/**
 * Recipes added to the Stock Keeper order through a crafter profile, with the region they were placed in.
 * Keyed by recipe identity: Create allows each recipe only once per order and compares recipes the same way.
 */
public final class ClientPlacements {
    private static final Map<Recipe<?>, Placement> PLACEMENTS = new IdentityHashMap<>();

    private ClientPlacements() {
    }

    public static synchronized void put(Recipe<?> recipe, Placement placement) {
        PLACEMENTS.put(recipe, placement);
    }

    @Nullable
    public static synchronized Placement get(Recipe<?> recipe) {
        return PLACEMENTS.get(recipe);
    }

    public static synchronized void remove(Recipe<?> recipe) {
        PLACEMENTS.remove(recipe);
    }

    public static synchronized void clear() {
        PLACEMENTS.clear();
    }
}
