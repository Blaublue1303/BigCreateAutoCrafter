package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game checks of the Create integration against the real Create 6.0.10 code.
 * Run with {@code ./gradlew runGameTestServer} (headless, exits with the test result).
 */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class CreateCompatGameTests {
    private static final String ARENA = "empty7";

    private static BlockState crafterFacing(Direction facing) {
        return AllBlocks.MECHANICAL_CRAFTER.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, facing);
    }

    @GameTest(template = ARENA)
    public static void singleCrafterIsItsOwnGroup(GameTestHelper helper) {
        BlockPos rel = new BlockPos(3, 2, 3);
        helper.setBlock(rel, crafterFacing(Direction.NORTH));
        BlockPos abs = helper.absolutePos(rel);

        Optional<ConnectedCrafterGroup> group = CreateCompat.findConnectedGroup(helper.getLevel(), abs);
        helper.assertTrue(group.isPresent(), "No group found for a single crafter");
        helper.assertTrue(group.get().controller().equals(abs), "Single crafter must be its own controller");
        helper.assertTrue(group.get().members().equals(List.of(abs)), "Single crafter group must contain only itself");
        helper.assertTrue(CreateCompat.findConnectedGroup(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))).isEmpty(),
                "Air must not be detected as a crafter group");
        helper.succeed();
    }

    /** 2×2 group facing north, connected the same way Create's wrench does it. */
    @GameTest(template = ARENA)
    public static void readsConnectedGroupInCreateOrder(GameTestHelper helper) {
        BlockPos bottomLeft = new BlockPos(2, 2, 3);
        BlockPos bottomRight = new BlockPos(3, 2, 3);
        BlockPos topLeft = new BlockPos(2, 3, 3);
        BlockPos topRight = new BlockPos(3, 3, 3);
        for (BlockPos rel : List.of(bottomLeft, bottomRight, topLeft, topRight)) {
            helper.setBlock(rel, crafterFacing(Direction.NORTH));
        }
        var level = helper.getLevel();
        ConnectedInputHandler.toggleConnection(level, helper.absolutePos(bottomLeft), helper.absolutePos(bottomRight));
        ConnectedInputHandler.toggleConnection(level, helper.absolutePos(bottomRight), helper.absolutePos(topRight));
        ConnectedInputHandler.toggleConnection(level, helper.absolutePos(bottomLeft), helper.absolutePos(topLeft));

        BlockPos expectedController = helper.absolutePos(bottomLeft);
        ConnectedCrafterGroup first = null;
        for (BlockPos rel : List.of(bottomLeft, bottomRight, topLeft, topRight)) {
            Optional<ConnectedCrafterGroup> group = CreateCompat.findConnectedGroup(level, helper.absolutePos(rel));
            helper.assertTrue(group.isPresent(), "No group found from member " + rel);
            helper.assertTrue(group.get().controller().equals(expectedController), "Wrong controller from member " + rel);
            helper.assertTrue(group.get().size() == 4, "Expected 4 members but got " + group.get().size());
            helper.assertTrue(group.get().facing() == Direction.NORTH, "Wrong facing " + group.get().facing());
            if (first == null) {
                first = group.get();
            } else {
                helper.assertTrue(first.equals(group.get()), "Group differs depending on the clicked member");
            }
        }

        // Create fills pattern slots top row first (limit L7). Column direction is logged as input for test T5.
        List<BlockPos> members = first.members();
        int topY = helper.absolutePos(topLeft).getY();
        helper.assertTrue(members.get(0).getY() == topY && members.get(1).getY() == topY,
                "Create order must start with the top row: " + members);
        Map<BlockPos, String> labels = new HashMap<>();
        for (BlockPos rel : List.of(bottomLeft, bottomRight, topLeft, topRight)) {
            BlockPos abs = helper.absolutePos(rel);
            labels.put(abs, "(x=" + abs.getX() + ", y=" + abs.getY() + ")");
        }
        BigCreateAutoCrafter.LOGGER.info("[T5 data] facing NORTH, Create inventory order (absolute x/y, +x = east): {}",
                members.stream().map(labels::get).toList());
        helper.succeed();
    }
}
