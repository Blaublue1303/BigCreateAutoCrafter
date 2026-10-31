package com.blaublue1303.bigcreateautocrafter.compat.create;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Snapshot of one connected Mechanical Crafter group as Create sees it.
 *
 * @param controller absolute position of the controller crafter
 * @param facing     horizontal facing of the controller
 * @param members    absolute positions of all crafters, in exactly the order of Create's
 *                   {@code ConnectedInput.getInventories(...)} (= the order in which Create's crafter unpacking
 *                   handler fills recipe pattern slots, see limit L6/L7 in docs/02-phase2-flow-analysis.md)
 */
public record ConnectedCrafterGroup(BlockPos controller, Direction facing, List<BlockPos> members) {
    public ConnectedCrafterGroup {
        controller = controller.immutable();
        members = List.copyOf(members);
    }

    public int size() {
        return members.size();
    }
}
