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
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
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

    /**
     * {@code false} if the crafter at {@code pos} belongs to a group of which Create can currently not reach every
     * member: the controller still lists a crafter whose block entity is gone (removed without Create noticing, e.g.
     * by another mod). Unloaded chunks are not a cause: on the server thread reading a block entity loads its chunk.
     * Create's {@code getInventories} silently drops the missing crafters, so every later pattern slot would land one
     * crafter too early.
     */
    public static boolean isGroupComplete(Level level, BlockPos pos) {
        MechanicalCrafterBlockEntity clicked = CrafterHelper.getCrafter(level, pos);
        if (clicked == null) {
            return true;
        }
        BlockPos controllerPos = controllerOf(clicked);
        MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(level, controllerPos);
        if (controller == null) {
            return false;
        }
        ConnectedInputHandler.ConnectedInput input = controller.getInput();
        ConnectedInputAccessor accessor = (ConnectedInputAccessor) input;
        if (!accessor.bcac$isController()) {
            return false;
        }
        int expected;
        synchronized (accessor.bcac$getData()) {
            expected = accessor.bcac$getData().size();
        }
        return input.getInventories(level, controllerPos).size() == expected;
    }

    /**
     * Index (in the group's member order = Create's inventory order) of the crafter where the crafted item leaves the
     * group: the one whose target arrow points at something that is not a crafter of the group.
     *
     * @return {@code -1} if there is not exactly one such crafter
     */
    public static int exitIndex(Level level, ConnectedCrafterGroup group) {
        int exit = -1;
        for (int i = 0; i < group.members().size(); i++) {
            BlockPos member = group.members().get(i);
            BlockState state = level.getBlockState(member);
            if (!isMechanicalCrafter(state)) {
                return -1;
            }
            BlockPos target = member.relative(MechanicalCrafterBlock.getTargetDirection(state));
            if (!group.members().contains(target)) {
                if (exit >= 0) {
                    return -1;
                }
                exit = i;
            }
        }
        return exit;
    }

    /** Position of the controller of the crafter group at {@code pos}; empty if there is no crafter there. */
    public static Optional<BlockPos> controllerPos(Level level, BlockPos pos) {
        MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(level, pos);
        return crafter == null ? Optional.empty() : Optional.of(controllerOf(crafter));
    }

    /**
     * Whether the chunks of the group at {@code pos} (controller and every member) are loaded. Reading a crafter in an
     * unloaded chunk would load that chunk, which a periodic check must not do.
     */
    public static boolean isGroupLoaded(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(level, pos);
        if (crafter == null) {
            return true; // nothing more to read: the caller finds no group
        }
        BlockPos controllerPos = controllerOf(crafter);
        if (!level.isLoaded(controllerPos)) {
            return false;
        }
        MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(level, controllerPos);
        if (controller == null) {
            return true;
        }
        ConnectedInputAccessor accessor = (ConnectedInputAccessor) controller.getInput();
        synchronized (accessor.bcac$getData()) {
            for (BlockPos offset : accessor.bcac$getData()) {
                if (!level.isLoaded(controllerPos.offset(offset))) {
                    return false;
                }
            }
        }
        return true;
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
