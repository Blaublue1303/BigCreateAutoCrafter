package com.blaublue1303.bigcreateautocrafter.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.blaublue1303.bigcreateautocrafter.recipe.PatternBuilder;
import com.blaublue1303.bigcreateautocrafter.recipe.RegionCraftable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Limit L4: when sending an order, Create turns every recipe into a 3×3 pattern. Entries added for a crafter region get a
 * pattern for that region instead (one slot per crafter, in Create's crafter order).
 */
@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {
    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelScreen;convertRecipeToPackageOrderContext(Lnet/minecraft/world/item/crafting/CraftingRecipe;Ljava/util/List;Z)Ljava/util/List;"))
    private List<BigItemStack> bcac$patternForRegion(CraftingRecipe recipe, List<BigItemStack> inputs, boolean respectAmounts,
                                                     Operation<List<BigItemStack>> original, @Local CraftableBigItemStack entry) {
        if (entry instanceof RegionCraftable regionEntry) {
            return PatternBuilder.build(regionEntry.placement.template(), inputs, respectAmounts);
        }
        return original.call(recipe, inputs, respectAmounts);
    }
}
