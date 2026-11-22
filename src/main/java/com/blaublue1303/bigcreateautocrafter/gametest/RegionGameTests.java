package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.profile.RegionRefresh;
import com.blaublue1303.bigcreateautocrafter.profile.RegionScanner;
import com.blaublue1303.bigcreateautocrafter.recipe.GridRecipe;
import com.blaublue1303.bigcreateautocrafter.recipe.Placement;
import com.blaublue1303.bigcreateautocrafter.recipe.RegionPlacer;
import com.mojang.serialization.DataResult;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;

import net.createmod.catnip.math.Pointing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Region scanning from real Create crafter groups, and recipe placement into regions. */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class RegionGameTests {

    @GameTest(template = TestCrafters.ARENA)
    public static void scans21CrafterGroup(GameTestHelper helper) {
        List<String> layout = ProfileCodecGameTests.LAYOUT_21;
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(layout));
        DataResult<List<String>> scanned = RegionScanner.layoutOf(group, 9, 9);
        helper.assertTrue(scanned.result().isPresent(), "Scan failed: " + scanned.error().map(e -> e.message()).orElse(""));
        helper.assertTrue(scanned.getOrThrow().equals(layout), "Unexpected layout " + scanned.getOrThrow());
        helper.assertTrue(RegionScanner.layoutOf(group, 4, 9).error().isPresent(), "Size limit not enforced");
        helper.succeed();
    }

    /** Asymmetric L-shape, both horizontal axes: the scanner's built-in check compares against Create's real order. */
    @GameTest(template = TestCrafters.ARENA)
    public static void scansGroupsWithGapsOnBothAxes(GameTestHelper helper) {
        List<String> lShape = List.of("#.", "##");
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
            List<BlockPos> rel = facing.getAxis() == Direction.Axis.Z
                    ? TestCrafters.positionsXY(lShape)
                    : TestCrafters.positionsZY(lShape).stream().map(p -> p.above(3)).toList(); // keep both groups apart
            ConnectedCrafterGroup group = TestCrafters.build(helper, facing, rel);
            DataResult<List<String>> scanned = RegionScanner.layoutOf(group, 9, 9);
            helper.assertTrue(scanned.result().isPresent(), facing + ": scan failed: " + scanned.error().map(e -> e.message()).orElse(""));
            List<String> result = scanned.getOrThrow();
            CrafterRegion region = new CrafterRegion("t", group.controller(), facing, result, "");
            helper.assertTrue(region.width() == 2 && region.height() == 2 && region.crafterCount() == 3, facing + ": wrong size " + result);
            helper.assertTrue(result.get(0).chars().filter(c -> c == '#').count() == 1 && result.get(1).equals("##"),
                    facing + ": gap must be in the top row: " + result);
        }
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void placesRecipesIntoRegions(GameTestHelper helper) {
        CrafterProfile profile = ProfileCodecGameTests.sampleProfile(); // regions: [5×5 (21), 3×3]
        Ingredient a = Ingredient.of(Items.IRON_INGOT);

        // 3×3 full recipe: auto -> the 3×3 region (fewest crafters)
        GridRecipe full3 = new GridRecipe(3, 3, Collections.nCopies(9, a), false);
        Optional<Placement> auto = RegionPlacer.place(full3, profile, -1);
        helper.assertTrue(auto.isPresent() && auto.get().regionIndex() == 1 && auto.get().template().size() == 9, "3×3 should go to the 3×3 region");

        // forced into the 21 region: corner (0,0) is a gap, so the recipe moves one column right
        Optional<Placement> big = RegionPlacer.place(full3, profile, 0);
        helper.assertTrue(big.isPresent() && big.get().template().size() == 21, "3×3 should fit into the 21 region");
        List<Ingredient> t = big.get().template();
        // row 0 has 3 crafters (cols 1-3) -> all recipe; row 1 has 5 crafters (cols 0-4) -> empty, 3 recipe, empty
        helper.assertTrue(!t.get(0).isEmpty() && !t.get(1).isEmpty() && !t.get(2).isEmpty(), "Row 0 not filled: " + t);
        helper.assertTrue(t.get(3).isEmpty() && !t.get(4).isEmpty() && !t.get(6).isEmpty() && t.get(7).isEmpty(), "Row 1 wrong: " + t);
        helper.assertTrue(t.stream().filter(i -> !i.isEmpty()).count() == 9, "Template must contain 9 items");

        // 5×5 full recipe cannot use the corner gaps
        GridRecipe full5 = new GridRecipe(5, 5, Collections.nCopies(25, a), false);
        helper.assertTrue(RegionPlacer.place(full5, profile, -1).isEmpty(), "Full 5×5 must not fit a 21 region");

        // shapeless with 4 ingredients fits a 2-wide region as 2×2
        CrafterRegion narrow = new CrafterRegion("2x2", BlockPos.ZERO, Direction.NORTH, List.of("##", "##"), "");
        GridRecipe shapeless4 = new GridRecipe(0, 0, Collections.nCopies(4, a), true);
        helper.assertTrue(RegionPlacer.templateFor(shapeless4, narrow).isPresent(), "Shapeless 4 must fit 2×2");
        helper.assertTrue(RegionPlacer.templateFor(new GridRecipe(0, 0, Collections.nCopies(5, a), true), narrow).isEmpty(),
                "Shapeless 5 must not fit 2×2");
        helper.succeed();
    }

    /** The exit is the crafter whose arrow leaves the group; it follows the arrows, not the position. */
    @GameTest(template = TestCrafters.ARENA)
    public static void findsExitCrafterFromArrows(GameTestHelper helper) {
        List<String> column = List.of("#", "#", "#");
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(column));
        // Create's default arrows all point up: the chain ends in the top crafter (index 0, rows run from the top)
        helper.assertTrue(CreateCompat.exitIndex(helper.getLevel(), group) == 0,
                "Default arrows: exit must be the top crafter, got " + CreateCompat.exitIndex(helper.getLevel(), group));
        for (BlockPos member : group.members()) {
            helper.getLevel().setBlockAndUpdate(member,
                    helper.getLevel().getBlockState(member).setValue(MechanicalCrafterBlock.POINTING, Pointing.DOWN));
        }
        helper.assertTrue(CreateCompat.exitIndex(helper.getLevel(), group) == 2,
                "Arrows down: exit must be the bottom crafter, got " + CreateCompat.exitIndex(helper.getLevel(), group));
        helper.succeed();
    }

    /** Small recipes sit next to the exit crafter instead of in the top-left corner. */
    @GameTest(template = TestCrafters.ARENA)
    public static void placesSmallRecipeNextToExit(GameTestHelper helper) {
        Ingredient a = Ingredient.of(Items.STONE);
        GridRecipe full3 = new GridRecipe(3, 3, Collections.nCopies(9, a), false);
        List<String> layout = ProfileCodecGameTests.LAYOUT_21;
        // 21 crafters read row by row; the bottom row ".###." holds indices 18..20, so 19 is its middle crafter
        CrafterRegion bottomExit = new CrafterRegion("21", BlockPos.ZERO, Direction.NORTH, layout, "", 19);
        helper.assertTrue(java.util.Arrays.equals(bottomExit.exitCell(), new int[] {2, 4}), "Exit cell of index 19 must be (2,4)");
        List<Ingredient> t = RegionPlacer.templateFor(full3, bottomExit).orElseThrow();
        helper.assertTrue(t.stream().filter(i -> !i.isEmpty()).count() == 9, "Template must contain 9 items");
        // the last three crafters (bottom row) are part of the recipe, the whole top row (indices 0..2) stays empty
        helper.assertTrue(!t.get(18).isEmpty() && !t.get(19).isEmpty() && !t.get(20).isEmpty(), "Bottom row must hold the recipe: " + t);
        helper.assertTrue(t.get(0).isEmpty() && t.get(1).isEmpty() && t.get(2).isEmpty(), "Top row must stay empty: " + t);

        // unknown exit (old profiles): unchanged, the recipe goes to the first fitting place from the top-left
        CrafterRegion unknown = new CrafterRegion("21", BlockPos.ZERO, Direction.NORTH, layout, "");
        List<Ingredient> old = RegionPlacer.templateFor(full3, unknown).orElseThrow();
        helper.assertTrue(unknown.exitCell() == null && !old.get(0).isEmpty(), "Unknown exit must keep the top-left placement: " + old);

        // a recipe that needs the whole region is not affected by the exit
        GridRecipe wide = new GridRecipe(5, 3, Collections.nCopies(15, a), false);
        helper.assertTrue(RegionPlacer.templateFor(wide, bottomExit).isEmpty() == RegionPlacer.templateFor(wide, unknown).isEmpty(),
                "Fit/no fit must not depend on the exit");
        helper.succeed();
    }

    /** A stored region that no longer matches the crafters is corrected from the world, a matching one stays untouched. */
    @GameTest(template = TestCrafters.ARENA)
    public static void healsStaleRegion(GameTestHelper helper) {
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(List.of("#", "#", "#")));
        CrafterRegion stale = new CrafterRegion("Test", group.controller(), Direction.NORTH, List.of("##", "##"), "keep");
        RegionRefresh.Result first = RegionRefresh.check(helper.getLevel(), stale);
        helper.assertTrue(first.state() == RegionRefresh.State.UPDATED && first.region().layout().equals(List.of("#", "#", "#")),
                "Stale shape must be replaced by the real one: " + first);
        helper.assertTrue(first.region().name().equals("Test") && first.region().address().equals("keep"), "Name and address must stay");
        helper.assertTrue(first.region().exit() >= 0, "The exit must be filled in for old regions");
        helper.assertTrue(RegionRefresh.check(helper.getLevel(), first.region()).state() == RegionRefresh.State.UNCHANGED,
                "A matching region must stay unchanged");
        helper.succeed();
    }

    /** A group with a missing crafter is reported as incomplete, so nothing is unpacked into shifted slots. */
    @GameTest(template = TestCrafters.ARENA)
    public static void detectsIncompleteGroup(GameTestHelper helper) {
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(List.of("#", "#", "#")));
        helper.assertTrue(CreateCompat.isGroupComplete(helper.getLevel(), group.controller()), "A fresh group is complete");
        // a member Create cannot reach (what an unloaded chunk looks like): its offset is in the controller's list, its crafter is not found
        var controller = com.simibubi.create.content.kinetics.crafter.CrafterHelper.getCrafter(helper.getLevel(), group.controller());
        ((com.blaublue1303.bigcreateautocrafter.mixin.ConnectedInputAccessor) controller.getInput()).bcac$getData().add(new BlockPos(7, 0, 0));
        helper.assertTrue(!CreateCompat.isGroupComplete(helper.getLevel(), group.controller()), "A missing crafter must make the group incomplete");
        helper.succeed();
    }

    /** Real recipes from Create and vanilla through the recipe manager. */
    @GameTest(template = TestCrafters.ARENA)
    public static void placesRealRecipes(GameTestHelper helper) {
        CrafterProfile profile = ProfileCodecGameTests.sampleProfile();
        GridRecipe crushingWheel = gridOf(helper, ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel"));
        helper.assertTrue(crushingWheel.width() == 5 && crushingWheel.height() == 5 && crushingWheel.cells().stream().filter(i -> !i.isEmpty()).count() == 21,
                "Crushing wheel is not 5×5 with 21 ingredients");
        Optional<Placement> wheel = RegionPlacer.place(crushingWheel, profile, -1);
        helper.assertTrue(wheel.isPresent() && wheel.get().regionIndex() == 0, "Crushing wheel must go to the 21 region");
        helper.assertTrue(wheel.get().template().stream().noneMatch(Ingredient::isEmpty), "Crushing wheel must use all 21 crafters");

        GridRecipe chest = gridOf(helper, ResourceLocation.withDefaultNamespace("chest"));
        Optional<Placement> chestPlacement = RegionPlacer.place(chest, profile, -1);
        helper.assertTrue(chestPlacement.isPresent() && chestPlacement.get().regionIndex() == 1, "Chest must go to the 3×3 region");

        CrafterProfile onlyBig = new CrafterProfile(UUID.randomUUID(), "x", Level.OVERWORLD, UUID.randomUUID(), List.of(profile.regions().get(0)));
        helper.assertTrue(RegionPlacer.place(chest, onlyBig, -1).isPresent(), "Chest must also fit into the 21 region");
        helper.succeed();
    }

    private static GridRecipe gridOf(GameTestHelper helper, ResourceLocation id) {
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new IllegalStateException("Recipe not found: " + id));
        if (!(holder.value() instanceof CraftingRecipe recipe)) {
            throw new IllegalStateException("Not a crafting recipe: " + id);
        }
        return GridRecipe.of(recipe).orElseThrow(() -> new IllegalStateException("No grid for " + id));
    }
}
