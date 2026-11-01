package com.blaublue1303.bigcreateautocrafter.compat.jei;

import java.util.Collection;
import java.util.List;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.client.ProfileSelector;
import com.simibubi.create.compat.jei.StockKeeperTransferHandler;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI entry point (only loaded by JEI, so JEI stays an optional dependency). */
@JeiPlugin
public class BcacJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "jei_plugin");

    /** Crafting categories shown for a Stock Keeper item: vanilla crafting and Create's two crafter categories. */
    private static List<RecipeType<RecipeHolder<CraftingRecipe>>> craftingTypes() {
        return List.of(
                RecipeTypes.CRAFTING,
                RecipeType.<CraftingRecipe>createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", "automatic_shaped")),
                RecipeType.<CraftingRecipe>createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting")));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        StockKeeperTransferHandler createHandler = new StockKeeperTransferHandler(registration.getJeiHelpers());
        for (RecipeType<RecipeHolder<CraftingRecipe>> type : craftingTypes()) {
            registration.addRecipeTransferHandler(new RegionTransferHandler(type, registration.getTransferHelper(), createHandler), type);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return ProfileSelector.extraAreas();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiRuntimeHolder.set(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRuntimeHolder.set(null);
    }
}
