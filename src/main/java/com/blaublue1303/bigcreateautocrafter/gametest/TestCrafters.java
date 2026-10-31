package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.ArrayList;
import java.util.List;

import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/** Builds connected Mechanical Crafter groups in GameTests, the same way Create's wrench connects them. */
final class TestCrafters {
    private TestCrafters() {
    }

    /** Relative positions of a flat group in the x/y plane at z = 3 (for facing NORTH/SOUTH) from a text layout. */
    static List<BlockPos> positionsXY(List<String> layout) {
        return positionsXY(layout, 3);
    }

    /** Same as {@link #positionsXY(List)} in the plane {@code z}. */
    static List<BlockPos> positionsXY(List<String> layout, int z) {
        List<BlockPos> positions = new ArrayList<>();
        for (int row = 0; row < layout.size(); row++) {
            for (int col = 0; col < layout.get(row).length(); col++) {
                if (layout.get(row).charAt(col) == '#') {
                    positions.add(new BlockPos(1 + col, 1 + layout.size() - 1 - row, z));
                }
            }
        }
        return positions;
    }

    /** Relative positions in the z/y plane at x = 3 (for facing EAST/WEST). */
    static List<BlockPos> positionsZY(List<String> layout) {
        return positionsXY(layout).stream().map(p -> new BlockPos(3, p.getY(), p.getX())).toList();
    }

    /** Places crafters at {@code relative} positions, connects them all to the first one and returns the group. */
    static ConnectedCrafterGroup build(GameTestHelper helper, Direction facing, List<BlockPos> relative) {
        for (BlockPos rel : relative) {
            helper.setBlock(rel, AllBlocks.MECHANICAL_CRAFTER.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, facing));
        }
        BlockPos first = helper.absolutePos(relative.get(0));
        for (int i = 1; i < relative.size(); i++) {
            ConnectedInputHandler.toggleConnection(helper.getLevel(), first, helper.absolutePos(relative.get(i)));
        }
        ConnectedCrafterGroup group = CreateCompat.findConnectedGroup(helper.getLevel(), first)
                .orElseThrow(() -> new IllegalStateException("No crafter group at " + first));
        if (group.size() != relative.size()) {
            throw new IllegalStateException("Expected " + relative.size() + " connected crafters, got " + group.size());
        }
        return group;
    }
}
