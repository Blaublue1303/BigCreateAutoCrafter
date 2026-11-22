package com.blaublue1303.bigcreateautocrafter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.blaublue1303.bigcreateautocrafter.order.SplitOrderUnpacking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The unpacking handler only gets the recipe context of a package, not its order id. Here the whole package is known,
 * so packages of one order from several packagers can share the recipe (see {@link SplitOrderUnpacking}).
 */
@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin {
    @WrapOperation(method = "unwrapBox", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/api/packager/unpacking/UnpackingHandler;unpack(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Ljava/util/List;Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;Z)Z"))
    private boolean bcac$shareRecipeWithinOrder(UnpackingHandler handler, Level level, BlockPos target, BlockState state, Direction side,
                                                List<ItemStack> items, PackageOrderWithCrafts context, boolean simulate,
                                                Operation<Boolean> original, @Local(argsOnly = true) ItemStack box) {
        return SplitOrderUnpacking.unpack(level, target, box, context, simulate, handler == CrafterUnpackingHandler.INSTANCE,
                usedContext -> original.call(handler, level, target, state, side, items, usedContext, simulate));
    }
}
