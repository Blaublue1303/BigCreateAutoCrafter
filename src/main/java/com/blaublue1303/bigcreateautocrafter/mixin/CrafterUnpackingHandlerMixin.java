package com.blaublue1303.bigcreateautocrafter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.blaublue1303.bigcreateautocrafter.order.LargePatternPackaging;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** For large patterns delivered in several packages, start crafting only when the whole pattern is in the crafters. */
@Mixin(CrafterUnpackingHandler.class)
public abstract class CrafterUnpackingHandlerMixin {
    @WrapOperation(method = "unpack", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/crafter/MechanicalCrafterBlockEntity;checkCompletedRecipe(Z)V"))
    private void bcac$waitForCompletePattern(MechanicalCrafterBlockEntity crafter, boolean poweredStart, Operation<Void> original,
                                             @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos,
                                             @Local(argsOnly = true) PackageOrderWithCrafts orderContext) {
        if (LargePatternPackaging.shouldStartCrafting(level, pos, crafter, orderContext)) {
            original.call(crafter, poweredStart);
        }
    }
}
