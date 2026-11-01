package com.blaublue1303.bigcreateautocrafter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.blaublue1303.bigcreateautocrafter.order.LargePatternPackaging;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;

import net.minecraft.util.RandomSource;

/**
 * Limit L5: per recipe entry, Create counts the possible crafts and then adds one package that holds only the first 9
 * pattern slots. For larger patterns that single {@code packages.add(...)} is replaced by complete packages; Create's own
 * counting and its handling of patterns up to 9 slots stay untouched.
 */
@Mixin(PackageRepackageHelper.class)
public abstract class PackageRepackageHelperMixin {
    @WrapOperation(method = "repackBasedOnRecipes", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean bcac$packLargePattern(List<BigItemStack> packages, Object createPackage, Operation<Boolean> original,
                                          @Local CraftingEntry craftingEntry, @Local(argsOnly = true) RandomSource random) {
        List<BigItemStack> pattern = craftingEntry.pattern().stacks();
        if (!LargePatternPackaging.isLarge(pattern)) {
            return original.call(packages, createPackage);
        }
        return packages.addAll(LargePatternPackaging.packagesFor(pattern, ((BigItemStack) createPackage).count, random));
    }
}
