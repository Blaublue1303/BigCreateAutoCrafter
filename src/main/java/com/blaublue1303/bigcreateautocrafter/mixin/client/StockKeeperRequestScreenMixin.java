package com.blaublue1303.bigcreateautocrafter.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blaublue1303.bigcreateautocrafter.client.ClientPlacements;
import com.blaublue1303.bigcreateautocrafter.recipe.PatternBuilder;
import com.blaublue1303.bigcreateautocrafter.recipe.Placement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Limit L4: when sending an order, Create turns every recipe into a 3×3 pattern. Recipes that were added for a crafter
 * region of a profile get a pattern for that region instead (one slot per crafter, in Create's crafter order).
 */
@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {
    @WrapOperation(method = "sendIt", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelScreen;convertRecipeToPackageOrderContext(Lnet/minecraft/world/item/crafting/CraftingRecipe;Ljava/util/List;Z)Ljava/util/List;"))
    private List<BigItemStack> bcac$patternForRegion(CraftingRecipe recipe, List<BigItemStack> inputs, boolean respectAmounts,
                                                     Operation<List<BigItemStack>> original) {
        Placement placement = ClientPlacements.get(recipe);
        if (placement == null) {
            return original.call(recipe, inputs, respectAmounts);
        }
        return PatternBuilder.build(placement.template(), inputs, respectAmounts);
    }

    /** Forget placements once Create has sent the order and emptied its recipe list. */
    @Inject(method = "sendIt", at = @At("TAIL"))
    private void bcac$forgetSentPlacements(CallbackInfo ci) {
        if (((StockKeeperRequestScreen) (Object) this).recipesToOrder.isEmpty()) {
            ClientPlacements.clear();
        }
    }
}
