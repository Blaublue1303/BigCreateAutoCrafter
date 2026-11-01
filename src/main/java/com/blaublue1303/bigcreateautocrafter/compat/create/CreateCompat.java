package com.blaublue1303.bigcreateautocrafter.compat.create;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blaublue1303.bigcreateautocrafter.mixin.ConnectedInputAccessor;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The only place that touches Create's Mechanical Crafter internals (risk R1: keep Create access in one package).
 */
public final class CreateCompat {
    private CreateCompat() {
    }

    public static boolean isMechanicalCrafter(BlockState state) {
        return AllBlocks.MECHANICAL_CRAFTER.has(state);
    }

    /**
     * Reads the connected crafter group that the crafter at {@code pos} belongs to.
     * Member order is taken from Create itself ({@code getInventories}), not re-implemented.
     *
     * @return empty if there is no crafter at {@code pos} or its controller is missing/inconsistent
     */
    public static Optional<ConnectedCrafterGroup> findConnectedGroup(Level level, BlockPos pos) {
        MechanicalCrafterBlockEntity clicked = CrafterHelper.getCrafter(level, pos);
        if (clicked == null) {
            return Optional.empty();
        }
        BlockPos controllerPos = controllerOf(clicked);
        MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(level, controllerPos);
        if (controller == null) {
            return Optional.empty();
        }
        ConnectedInputHandler.ConnectedInput input = controller.getInput();
        ConnectedInputAccessor accessor = (ConnectedInputAccessor) input;
        if (!accessor.bcac$isController()) {
            return Optional.empty();
        }

        // Create's own ordering: position of each member = index of its inventory in getInventories(...)
        List<MechanicalCrafterBlockEntity.Inventory> inventories = input.getInventories(level, controllerPos);
        Map<MechanicalCrafterBlockEntity.Inventory, Integer> createOrder = new IdentityHashMap<>();
        for (int i = 0; i < inventories.size(); i++) {
            createOrder.put(inventories.get(i), i);
        }
        BlockPos[] members = new BlockPos[inventories.size()];
        int found = 0;
        synchronized (accessor.bcac$getData()) {
            for (BlockPos offset : accessor.bcac$getData()) {
                BlockPos memberPos = controllerPos.offset(offset);
                MechanicalCrafterBlockEntity member = CrafterHelper.getCrafter(level, memberPos);
                Integer index = member == null ? null : createOrder.get(member.getInventory());
                if (index == null || members[index] != null) {
                    return Optional.empty();
                }
                members[index] = memberPos;
                found++;
            }
        }
        if (found != members.length) {
            return Optional.empty();
        }

        Direction facing = controller.getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        return Optional.of(new ConnectedCrafterGroup(controllerPos, facing, List.of(members)));
    }

    private static BlockPos controllerOf(MechanicalCrafterBlockEntity crafter) {
        ConnectedInputAccessor accessor = (ConnectedInputAccessor) crafter.getInput();
        if (accessor.bcac$isController()) {
            return crafter.getBlockPos();
        }
        List<BlockPos> data = accessor.bcac$getData();
        return data.isEmpty() ? crafter.getBlockPos() : crafter.getBlockPos().offset(data.get(0));
    }
}
