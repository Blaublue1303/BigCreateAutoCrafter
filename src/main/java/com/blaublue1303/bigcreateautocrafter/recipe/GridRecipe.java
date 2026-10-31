package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Recipe ingredients as a grid.
 *
 * @param width     grid width (shaped recipes only)
 * @param height    grid height (shaped recipes only)
 * @param cells     shaped: row-major cells of size width × height ({@link Ingredient#EMPTY} = no item);
 *                  shapeless: the non-empty ingredients
 * @param shapeless whether the layout of {@code cells} is free
 */
public record GridRecipe(int width, int height, List<Ingredient> cells, boolean shapeless) {
    public GridRecipe {
        cells = List.copyOf(cells);
    }

    /** Covers vanilla shaped/shapeless crafting and Create's mechanical crafting ({@code MechanicalCraftingRecipe extends ShapedRecipe}). */
    public static Optional<GridRecipe> of(CraftingRecipe recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() * shaped.getHeight() != ingredients.size()) {
                return Optional.empty();
            }
            return Optional.of(new GridRecipe(shaped.getWidth(), shaped.getHeight(), ingredients, false));
        }
        List<Ingredient> nonEmpty = ingredients.stream().filter(i -> !i.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            return Optional.empty(); // special recipes (fireworks, banners, ...) have no fixed ingredients
        }
        return Optional.of(new GridRecipe(0, 0, nonEmpty, true));
    }

    public int ingredientCount() {
        return (int) cells.stream().filter(i -> !i.isEmpty()).count();
    }
}
