package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Crafting with Create's Factory Gauge on a crafter region.
 *
 * <p>A Factory Gauge stores a crafting arrangement (one item per crafter slot) and sends it as pattern with every order to
 * its recipe address. Create builds that arrangement as 3×3 and only finds regular crafting recipes. For a gauge whose
 * recipe address is the address of a crafter region, the arrangement is built for that region instead, and mechanical
 * crafting recipes are found as well.
 */
public final class FactoryArrangements {
    private FactoryArrangements() {
    }

    /** The region (of the given profiles, in this dimension) whose package address is {@code address}. */
    public static Optional<CrafterRegion> regionForAddress(List<CrafterProfile> profiles, ResourceKey<Level> dimension, String address) {
        String wanted = address.trim();
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        return profiles.stream()
                .filter(p -> p.dimension().equals(dimension))
                .flatMap(p -> p.regions().stream())
                .filter(r -> r.address().equals(wanted))
                .findFirst();
    }

    /** Whether the recipe fits Create's own 3×3 arrangement. */
    public static boolean fitsThreeByThree(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() <= 3 && shaped.getHeight() <= 3;
        }
        return recipe.getIngredients().stream().filter(i -> !i.isEmpty()).count() <= 9;
    }

    /**
     * Arrangement for {@code region}: one stack (count 1) per crafter, picked from the gauge's inputs like Create does
     * ({@code convertRecipeToPackageOrderContext} with {@code respectAmounts = false}).
     *
     * @return empty if the recipe does not fit the region
     */
    public static Optional<List<ItemStack>> forRegion(CraftingRecipe recipe, List<BigItemStack> inputs, CrafterRegion region) {
        return GridRecipe.of(recipe)
                .flatMap(grid -> RegionPlacer.templateFor(grid, region))
                .map(template -> PatternBuilder.build(template, inputs, false).stream()
                        .map(slot -> slot.stack.isEmpty() ? ItemStack.EMPTY : slot.stack.copyWithCount(1))
                        .toList());
    }

    /**
     * A mechanical crafting recipe for {@code output} made from the gauge's inputs, with the same rules Create uses for
     * regular crafting recipes in the gauge: same result item, every ingredient available among the inputs, all input
     * item types used, not excluded from automation; {@code usable} decides which recipes can be crafted at all.
     */
    public static Optional<CraftingRecipe> findMechanicalRecipe(Level level, ItemStack output, List<BigItemStack> inputs,
                                                                Predicate<CraftingRecipe> usable) {
        if (output.isEmpty() || inputs.isEmpty()) {
            return Optional.empty();
        }
        Set<Item> itemsToUse = new HashSet<>();
        for (BigItemStack input : inputs) {
            if (!input.stack.isEmpty()) {
                itemsToUse.add(input.stack.getItem());
            }
        }
        List<RecipeHolder<CraftingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(AllRecipeTypes.MECHANICAL_CRAFTING.getType());
        return recipes.stream()
                .filter(holder -> holder.value().getResultItem(level.registryAccess()).is(output.getItem()))
                .filter(holder -> !AllRecipeTypes.shouldIgnoreInAutomation(holder))
                .map(RecipeHolder::value)
                .filter(recipe -> usesExactly(recipe, inputs, itemsToUse))
                .filter(usable)
                .findFirst();
    }

    private static boolean usesExactly(CraftingRecipe recipe, List<BigItemStack> inputs, Set<Item> itemsToUse) {
        Set<Item> itemsUsed = new HashSet<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            Optional<BigItemStack> match = inputs.stream().filter(input -> !input.stack.isEmpty() && ingredient.test(input.stack)).findFirst();
            if (match.isEmpty()) {
                return false;
            }
            itemsUsed.add(match.get().stack.getItem());
        }
        return itemsUsed.size() >= itemsToUse.size();
    }
}
