package com.blaublue1303.bigcreateautocrafter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;

import com.blaublue1303.bigcreateautocrafter.order.LargePatternPackaging;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** For large patterns delivered in several packages, start crafting only when the whole pattern is in the crafters. */
@Mixin(CrafterUnpackingHandler.class)
public abstract class CrafterUnpackingHandlerMixin {
    private static long bcac$lastWarning = Long.MIN_VALUE / 2;

    /**
     * Refuse to unpack while part of the crafter group is not reachable (unloaded chunk): Create would silently drop the
     * missing crafters and put every later item into the wrong crafter. The package stays in the packager and is
     * retried, so the recipe arrives correctly as soon as the whole group is loaded.
     */
    @Inject(method = "unpack", at = @At("HEAD"), cancellable = true)
    private void bcac$onlyUnpackIntoCompleteGroup(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
                                                  PackageOrderWithCrafts orderContext, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (PackageOrderWithCrafts.hasCraftingInformation(orderContext) && !CreateCompat.isGroupComplete(level, pos)) {
            if (level.getGameTime() - bcac$lastWarning > 1200) { // at most once a minute, the packager retries every few ticks
                bcac$lastWarning = level.getGameTime();
                BigCreateAutoCrafter.LOGGER.warn("Crafter group at {} is not fully loaded, package is kept back", pos);
            }
            cir.setReturnValue(false);
        }
    }

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
