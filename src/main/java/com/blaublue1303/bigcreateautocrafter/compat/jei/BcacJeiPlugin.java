package com.blaublue1303.bigcreateautocrafter.compat.jei;

import java.util.List;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.simibubi.create.compat.jei.StockKeeperTransferHandler;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI entry point (only loaded by JEI, so JEI stays an optional dependency). */
@JeiPlugin
public class BcacJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "jei_plugin");

    /** Create's crafter categories (Create builds them with {@code RecipeType.createRecipeHolderType} as well). */
    static final RecipeType<RecipeHolder<CraftingRecipe>> AUTOMATIC_SHAPED =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", "automatic_shaped"));
    static final RecipeType<RecipeHolder<CraftingRecipe>> MECHANICAL_CRAFTING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting"));

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /** Crafting categories shown for a Stock Keeper item: vanilla crafting and Create's two crafter categories. */
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        StockKeeperTransferHandler createHandler = new StockKeeperTransferHandler(registration.getJeiHelpers());
        for (RecipeType<RecipeHolder<CraftingRecipe>> type : List.of(RecipeTypes.CRAFTING, AUTOMATIC_SHAPED, MECHANICAL_CRAFTING)) {
            registration.addRecipeTransferHandler(new RegionTransferHandler(type, registration.getTransferHelper(), createHandler), type);
        }
    }
}
