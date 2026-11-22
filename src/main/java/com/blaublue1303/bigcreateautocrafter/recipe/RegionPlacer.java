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
 * on a crafter. With a known exit crafter the fitting offset closest to the exit is used (the result then has the
 * shortest way out of the group); without one, the first fitting offset from the top-left, like Create's own 3×3.
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
        int[] exit = region.exitCell();
        int bestX = -1;
        int bestY = -1;
        long bestScore = Long.MAX_VALUE;
        for (int offsetY = 0; offsetY + height <= region.height(); offsetY++) {
            for (int offsetX = 0; offsetX + width <= region.width(); offsetX++) {
                if (!covers(width, height, cells, region, offsetX, offsetY)) {
                    continue;
                }
                if (exit == null) {
                    return Optional.of(template(width, height, cells, region, offsetX, offsetY));
                }
                long score = distanceToExit(width, height, offsetX, offsetY, exit);
                if (score < bestScore) { // strictly less: the first offset from the top-left wins ties
                    bestScore = score;
                    bestX = offsetX;
                    bestY = offsetY;
                }
            }
        }
        return bestX < 0 ? Optional.empty() : Optional.of(template(width, height, cells, region, bestX, bestY));
    }

    /**
     * Smaller = closer to the exit crafter: first the distance from the exit to the recipe's bounding box, then the
     * distance from the box's centre to the exit (so a recipe sits next to the exit instead of in a far corner).
     */
    private static long distanceToExit(int width, int height, int offsetX, int offsetY, int[] exit) {
        int dx = Math.max(0, Math.max(offsetX - exit[0], exit[0] - (offsetX + width - 1)));
        int dy = Math.max(0, Math.max(offsetY - exit[1], exit[1] - (offsetY + height - 1)));
        int centreX2 = Math.abs(2 * offsetX + width - 1 - 2 * exit[0]);
        int centreY2 = Math.abs(2 * offsetY + height - 1 - 2 * exit[1]);
        return (long) (dx + dy) * 1000 + centreX2 + centreY2;
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
