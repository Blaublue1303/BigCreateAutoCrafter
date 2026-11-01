package com.blaublue1303.bigcreateautocrafter.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** In-game checks of the JEI integration for the client smoke test (dev only). */
public final class JeiSmokeCheck {
    private JeiSmokeCheck() {
    }

    /** @return result lines starting with PASS or FAIL */
    public static List<String> check(StockKeeperRequestMenu menu) {
        List<String> results = new ArrayList<>();
        IJeiRuntime runtime = CreateJEI.runtime;
        if (runtime == null) {
            results.add("FAIL JEI runtime not available");
            return results;
        }
        for (RecipeType<RecipeHolder<CraftingRecipe>> type : List.of(RecipeTypes.CRAFTING, BcacJeiPlugin.MECHANICAL_CRAFTING)) {
            try {
                IRecipeCategory<RecipeHolder<CraftingRecipe>> category = runtime.getRecipeManager().getRecipeCategory(type);
                Optional<IRecipeTransferHandler<StockKeeperRequestMenu, RecipeHolder<CraftingRecipe>>> handler =
                        runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category);
                boolean ours = handler.isPresent() && handler.get() instanceof RegionTransferHandler;
                results.add((ours ? "PASS" : "FAIL") + " JEI handler for " + type.getUid() + ": "
                        + handler.map(h -> h.getClass().getName()).orElse("none"));
            } catch (RuntimeException e) {
                results.add("FAIL JEI handler lookup for " + type.getUid() + ": " + e);
            }
        }
        results.add(transferCrushingWheel(runtime, menu));
        return results;
    }

    /**
     * Runs the "+" check (no transfer) for Create's 21-ingredient crushing wheel recipe. Create's own handler rejects it
     * with an internal error (limit L1); with a profile selected our handler must accept the recipe and only report
     * missing stock (the test world has no items).
     */
    @SuppressWarnings("removal")
    private static String transferCrushingWheel(IJeiRuntime runtime, StockKeeperRequestMenu menu) {
        try {
            Minecraft mc = Minecraft.getInstance();
            @SuppressWarnings("unchecked")
            RecipeHolder<CraftingRecipe> recipe = (RecipeHolder<CraftingRecipe>) mc.level.getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel")).orElseThrow();
            IRecipeCategory<RecipeHolder<CraftingRecipe>> category = runtime.getRecipeManager().getRecipeCategory(BcacJeiPlugin.MECHANICAL_CRAFTING);
            IRecipeLayoutDrawable<RecipeHolder<CraftingRecipe>> layout = runtime.getRecipeManager()
                    .createRecipeLayoutDrawable(category, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup())
                    .orElseThrow();
            IRecipeTransferHandler<StockKeeperRequestMenu, RecipeHolder<CraftingRecipe>> handler =
                    runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category).orElseThrow();
            IRecipeTransferError error = handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), mc.player, false, false);
            boolean ok = error != null && error.getType() == IRecipeTransferError.Type.USER_FACING
                    && error.getClass().getSimpleName().contains("MissingSlots");
            return (ok ? "PASS" : "FAIL") + " crushing wheel (21 ingredients) accepted, only stock missing: "
                    + (error == null ? "no error" : error.getType() + " " + error.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return "FAIL crushing wheel transfer check: " + e;
        }
    }
}
