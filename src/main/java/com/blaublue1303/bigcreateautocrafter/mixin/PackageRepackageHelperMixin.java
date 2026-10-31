package com.blaublue1303.bigcreateautocrafter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.blaublue1303.bigcreateautocrafter.order.LargePatternPackaging;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

import net.minecraft.util.RandomSource;

/** Limit L5: Create cuts per-craft packages off after 9 pattern slots. Only orders with larger patterns are changed. */
@Mixin(PackageRepackageHelper.class)
public abstract class PackageRepackageHelperMixin {
    @Inject(method = "repackBasedOnRecipes", at = @At("HEAD"), cancellable = true)
    private void bcac$packLargePatterns(InventorySummary summary, PackageOrderWithCrafts order, String address, RandomSource random,
                                        CallbackInfoReturnable<List<BigItemStack>> cir) {
        if (LargePatternPackaging.hasLargePattern(order)) {
            cir.setReturnValue(LargePatternPackaging.repackBasedOnRecipes(summary, order, random));
        }
    }
}
