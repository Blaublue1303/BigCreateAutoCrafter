package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Turns a connected crafter group into a {@link CrafterRegion} layout.
 *
 * <p>Rows run from the highest Y down. Columns follow the axis and direction Create itself uses to sort crafter
 * inventories ({@code ConnectedInput.getInventories}). The result is checked against the order Create actually
 * returned, so a change in Create's ordering is detected instead of silently producing wrong patterns.
 */
public final class RegionScanner {
    private RegionScanner() {
    }

    /** A grid cell of the layout, 0-based from top-left. */
    public record Cell(int column, int row) {
    }

    public static DataResult<List<String>> layoutOf(ConnectedCrafterGroup group, int maxWidth, int maxHeight) {
        Direction facing = group.facing();
        Direction.Axis depthAxis = facing.getAxis();
        int depth = depthAxis.choose(group.controller().getX(), group.controller().getY(), group.controller().getZ());
        for (BlockPos member : group.members()) {
            if (depthAxis.choose(member.getX(), member.getY(), member.getZ()) != depth) {
                return DataResult.error(() -> "Crafter group is not flat");
            }
        }

        List<Cell> cells = cellsOf(group);
        int width = cells.stream().mapToInt(Cell::column).max().orElse(-1) + 1;
        int height = cells.stream().mapToInt(Cell::row).max().orElse(-1) + 1;
        if (width > maxWidth || height > maxHeight) {
            return DataResult.error(() -> "Crafter group is " + width + "×" + height + ", maximum is " + maxWidth + "×" + maxHeight);
        }

        char[][] grid = new char[height][width];
        for (char[] row : grid) {
            Arrays.fill(row, CrafterRegion.GAP);
        }
        for (Cell cell : cells) {
            grid[cell.row()][cell.column()] = CrafterRegion.CRAFTER;
        }

        // Reading the layout row by row must reproduce Create's own inventory order.
        List<Cell> rowMajor = new ArrayList<>(cells);
        rowMajor.sort(Comparator.comparingInt(Cell::row).thenComparingInt(Cell::column));
        if (!rowMajor.equals(cells)) {
            return DataResult.error(() -> "Create's crafter order does not match the expected layout order");
        }

        List<String> layout = new ArrayList<>(height);
        for (char[] row : grid) {
            layout.add(new String(row));
        }
        return DataResult.success(layout);
    }

    /**
     * Grid cell of every member, in the member order of {@code group} (= Create's inventory order).
     * Mirrors Create 6.0.10's comparator: Y descending, then along {@code facing.getClockWise().getAxis()} with
     * sign {@code facing.getAxisDirection().getStep() * (axis == Z ? -1 : 1)}.
     */
    public static List<Cell> cellsOf(ConnectedCrafterGroup group) {
        Direction facing = group.facing();
        Direction.Axis columnAxis = facing.getClockWise().getAxis();
        int sign = facing.getAxisDirection().getStep() * (columnAxis == Direction.Axis.Z ? -1 : 1);

        int maxY = Integer.MIN_VALUE;
        int minKey = Integer.MAX_VALUE;
        for (BlockPos member : group.members()) {
            maxY = Math.max(maxY, member.getY());
            minKey = Math.min(minKey, sign * columnAxis.choose(member.getX(), member.getY(), member.getZ()));
        }
        List<Cell> cells = new ArrayList<>(group.size());
        for (BlockPos member : group.members()) {
            int key = sign * columnAxis.choose(member.getX(), member.getY(), member.getZ());
            cells.add(new Cell(key - minKey, maxY - member.getY()));
        }
        return cells;
    }
}
