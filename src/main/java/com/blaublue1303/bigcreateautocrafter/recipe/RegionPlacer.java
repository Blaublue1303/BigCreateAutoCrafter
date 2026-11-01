package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * Fits a {@link GridRecipe} into a crafter region.
 *
 * <p>Create crafts from the bounding box of the filled crafters (empty crafters around it are allowed when the group
 * is started by a package), so the recipe may sit anywhere in the region as long as every non-empty recipe cell lands
 * on a crafter. The first fitting offset from the top-left is used, matching Create's own 3×3 behaviour.
 */
public final class RegionPlacer {
    private RegionPlacer() {
    }

    /**
     * @param regionIndex -1 = automatic: the region with the fewest crafters that fits (ties: profile order)
     */
    public static Optional<Placement> place(GridRecipe recipe, CrafterProfile profile, int regionIndex) {
        List<CrafterRegion> regions = profile.regions();
        List<Integer> candidates = new ArrayList<>();
        if (regionIndex >= 0) {
            if (regionIndex < regions.size()) {
                candidates.add(regionIndex);
            }
        } else {
            int[] crafterCounts = new int[regions.size()];
            for (int i = 0; i < regions.size(); i++) {
                crafterCounts[i] = regions.get(i).crafterCount();
                candidates.add(i);
            }
            candidates.sort(Comparator.comparingInt(i -> crafterCounts[i]));
        }
        for (int index : candidates) {
            Optional<List<Ingredient>> template = templateFor(recipe, regions.get(index));
            if (template.isPresent()) {
                return Optional.of(new Placement(index, regions.get(index), template.get()));
            }
        }
        return Optional.empty();
    }

    /** @return the pattern template for {@code region}, or empty if the recipe does not fit */
    public static Optional<List<Ingredient>> templateFor(GridRecipe recipe, CrafterRegion region) {
        if (!recipe.shapeless()) {
            return fit(recipe.width(), recipe.height(), recipe.cells(), region);
        }
        int count = recipe.cells().size();
        for (int width = Math.min(count, region.width()); width >= 1; width--) {
            int height = (count + width - 1) / width;
            if (height > region.height()) {
                break;
            }
            List<Ingredient> cells = new ArrayList<>(width * height);
            for (int i = 0; i < width * height; i++) {
                cells.add(i < count ? recipe.cells().get(i) : Ingredient.EMPTY);
            }
            Optional<List<Ingredient>> template = fit(width, height, cells, region);
            if (template.isPresent()) {
                return template;
            }
        }
        return Optional.empty();
    }

    private static Optional<List<Ingredient>> fit(int width, int height, List<Ingredient> cells, CrafterRegion region) {
        for (int offsetY = 0; offsetY + height <= region.height(); offsetY++) {
            for (int offsetX = 0; offsetX + width <= region.width(); offsetX++) {
                if (covers(width, height, cells, region, offsetX, offsetY)) {
                    return Optional.of(template(width, height, cells, region, offsetX, offsetY));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean covers(int width, int height, List<Ingredient> cells, CrafterRegion region, int offsetX, int offsetY) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!cells.get(y * width + x).isEmpty() && !region.hasCrafterAt(offsetX + x, offsetY + y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Ingredient> template(int width, int height, List<Ingredient> cells, CrafterRegion region, int offsetX, int offsetY) {
        List<Ingredient> template = new ArrayList<>(region.crafterCount());
        for (int y = 0; y < region.height(); y++) {
            for (int x = 0; x < region.width(); x++) {
                if (!region.hasCrafterAt(x, y)) {
                    continue;
                }
                int rx = x - offsetX;
                int ry = y - offsetY;
                boolean inside = rx >= 0 && ry >= 0 && rx < width && ry < height;
                template.add(inside ? cells.get(ry * width + rx) : Ingredient.EMPTY);
            }
        }
        return template;
    }
}
