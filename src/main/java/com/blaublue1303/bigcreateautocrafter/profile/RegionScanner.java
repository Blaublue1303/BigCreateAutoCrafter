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
 * Turns a connected crafter group into a {@link CrafterRegion} layout whose {@code '#'} cells, read row by row, are in
 * exactly the order Create returns the crafter inventories ({@code ConnectedInput.getInventories}).
 */
public final class RegionScanner {
    private RegionScanner() {
    }

    /** A grid cell of the layout, 0-based from top-left. */
    public record Cell(int column, int row) {
    }

    public static DataResult<List<String>> layoutOf(ConnectedCrafterGroup group, int maxWidth, int maxHeight) {
        Direction.Axis depthAxis = group.facing().getAxis();
        int depth = group.controller().get(depthAxis);
        for (BlockPos member : group.members()) {
            if (member.get(depthAxis) != depth) {
                return DataResult.error(() -> "Crafter group is not flat");
            }
        }

        List<Cell> cells = cellsOf(group);
        if (cells.isEmpty()) {
            return DataResult.error(() -> "Create's crafter order does not match a row-by-row layout");
        }
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
        List<String> layout = new ArrayList<>(height);
        for (char[] row : grid) {
            layout.add(new String(row));
        }
        return DataResult.success(layout);
    }

    /**
     * Grid cell of every member, in the member order of {@code group} (= Create's inventory order), or an empty list if
     * that order is not row by row. Rows run from the highest Y down; columns run along the horizontal axis of the group's
     * plane, in whichever direction makes reading the cells row by row reproduce Create's order. The direction is taken
     * from Create's actual order instead of re-implementing its private comparator.
     */
    public static List<Cell> cellsOf(ConnectedCrafterGroup group) {
        Direction.Axis columnAxis = group.facing().getClockWise().getAxis();
        for (int sign : new int[] {1, -1}) {
            List<Cell> cells = cellsWithColumnSign(group, columnAxis, sign);
            if (isRowByRow(cells)) {
                return cells;
            }
        }
        return List.of();
    }

    private static List<Cell> cellsWithColumnSign(ConnectedCrafterGroup group, Direction.Axis columnAxis, int sign) {
        int maxY = Integer.MIN_VALUE;
        int minColumn = Integer.MAX_VALUE;
        for (BlockPos member : group.members()) {
            maxY = Math.max(maxY, member.getY());
            minColumn = Math.min(minColumn, sign * member.get(columnAxis));
        }
        List<Cell> cells = new ArrayList<>(group.size());
        for (BlockPos member : group.members()) {
            cells.add(new Cell(sign * member.get(columnAxis) - minColumn, maxY - member.getY()));
        }
        return cells;
    }

    private static boolean isRowByRow(List<Cell> cells) {
        List<Cell> sorted = new ArrayList<>(cells);
        sorted.sort(Comparator.comparingInt(Cell::row).thenComparingInt(Cell::column));
        return sorted.equals(cells);
    }
}
