package com.blaublue1303.bigcreateautocrafter.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;

import net.minecraft.core.BlockPos;

/**
 * Read access to the package-private fields of Create's {@code ConnectedInput} (Create 6.0.10).
 * {@code data} holds, on the controller, the offsets of all group members (incl. {@link BlockPos#ZERO});
 * on a member, a single offset pointing to the controller.
 */
@Mixin(ConnectedInputHandler.ConnectedInput.class)
public interface ConnectedInputAccessor {
    @Accessor("isController")
    boolean bcac$isController();

    @Accessor("data")
    List<BlockPos> bcac$getData();
}
