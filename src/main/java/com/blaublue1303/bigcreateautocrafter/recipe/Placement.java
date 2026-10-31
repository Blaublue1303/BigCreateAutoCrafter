package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.List;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * Where a recipe goes inside a region.
 *
 * @param profileId   profile the region belongs to
 * @param regionIndex index of the region in the profile
 * @param region      the region
 * @param template    one entry per crafter of the region, in Create's inventory order (= the pattern slot order Create's
 *                    crafter unpacking handler uses); {@link Ingredient#EMPTY} for crafters that stay empty
 */
public record Placement(UUID profileId, int regionIndex, CrafterRegion region, List<Ingredient> template) {
    public Placement {
        template = List.copyOf(template);
    }
}
