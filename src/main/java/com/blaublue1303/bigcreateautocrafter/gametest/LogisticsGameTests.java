package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.order.LargePatternPackaging;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.profile.RegionScanner;
import com.blaublue1303.bigcreateautocrafter.recipe.GridRecipe;
import com.blaublue1303.bigcreateautocrafter.recipe.PatternBuilder;
import com.blaublue1303.bigcreateautocrafter.recipe.RegionPlacer;
import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Prototype tests T3/T4 against real Create code: large patterns over the network, packaging in the repackager, and
 * Create's crafter unpacking handler filling a 21-crafter group.
 */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class LogisticsGameTests {
    private static final Item[] NINE = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.DIAMOND, Items.EMERALD,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.COAL, Items.QUARTZ};

    /** Exposes Create's protected repack step; calls go through our mixin. */
    private static final class ExposedRepackageHelper extends PackageRepackageHelper {
        List<BigItemStack> repack(InventorySummary summary, PackageOrderWithCrafts order) {
            return repackBasedOnRecipes(summary, order, "test", RandomSource.create(1));
        }
    }

    private static List<BigItemStack> pattern(Item item, int count) {
        List<BigItemStack> pattern = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pattern.add(new BigItemStack(new ItemStack(item), 1));
        }
        return pattern;
    }

    private static PackageOrderWithCrafts craftOrder(List<BigItemStack> pattern, int crafts) {
        return new PackageOrderWithCrafts(PackageOrder.empty(), List.of(new PackageOrderWithCrafts.CraftingEntry(new PackageOrder(pattern), crafts)));
    }

    /** T3: a 21-slot pattern survives Create's network codec unchanged. */
    @GameTest(template = TestCrafters.ARENA)
    public static void largePatternSurvivesNetwork(GameTestHelper helper) {
        List<BigItemStack> pattern = pattern(Items.IRON_INGOT, 21);
        pattern.set(0, new BigItemStack(ItemStack.EMPTY, 1));
        PackageOrderWithCrafts order = craftOrder(pattern, 3);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        PackageOrderWithCrafts.STREAM_CODEC.encode(buf, order);
        PackageOrderWithCrafts decoded = PackageOrderWithCrafts.STREAM_CODEC.decode(buf);
        List<BigItemStack> back = decoded.orderedCrafts().get(0).pattern().stacks();
        helper.assertTrue(back.size() == 21 && back.get(0).stack.isEmpty() && back.get(20).stack.is(Items.IRON_INGOT)
                && decoded.orderedCrafts().get(0).count() == 3, "Pattern changed over the network");
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void repackagerPacksLargePatternByItem(GameTestHelper helper) {
        List<BigItemStack> pattern = pattern(Items.IRON_INGOT, 16);
        pattern.addAll(pattern(Items.GOLD_INGOT, 5));
        InventorySummary summary = new InventorySummary();
        summary.add(new ItemStack(Items.IRON_INGOT), 40);
        summary.add(new ItemStack(Items.GOLD_INGOT), 10);

        List<BigItemStack> packages = new ExposedRepackageHelper().repack(summary, craftOrder(pattern, 3));
        helper.assertTrue(packages.size() == 1 && packages.get(0).count == 2, "Expected one package type ×2 (gold only lasts for 2 crafts): " + packages.size());
        ItemStackHandler contents = PackageItem.getContents(packages.get(0).stack);
        helper.assertTrue(contents.getStackInSlot(0).is(Items.IRON_INGOT) && contents.getStackInSlot(0).getCount() == 16
                && contents.getStackInSlot(1).is(Items.GOLD_INGOT) && contents.getStackInSlot(1).getCount() == 5, "Package not grouped by item");
        PackageOrderWithCrafts context = PackageItem.getOrderContext(packages.get(0).stack);
        helper.assertTrue(PackageOrderWithCrafts.hasCraftingInformation(context) && context.getCraftingInformation().size() == 21,
                "Package must carry the full 21-slot pattern");
        // Like Create: the failing 3rd craft has already taken the remaining 8 iron before gold ran out (no rollback).
        helper.assertTrue(summary.getCountOf(new ItemStack(Items.IRON_INGOT)) == 0 && summary.getCountOf(new ItemStack(Items.GOLD_INGOT)) == 0,
                "Summary must be reduced like Create does");
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void repackagerSplitsUnstackablesAndInterleaves(GameTestHelper helper) {
        List<BigItemStack> pattern = pattern(Items.WATER_BUCKET, 21);
        InventorySummary summary = new InventorySummary();
        summary.add(new ItemStack(Items.WATER_BUCKET), 42);
        List<BigItemStack> packages = new ExposedRepackageHelper().repack(summary, craftOrder(pattern, 2));
        helper.assertTrue(packages.size() == 6 && packages.stream().allMatch(p -> p.count == 1), "Expected 3 parts × 2 crafts: " + packages.size());
        int[] expectedSlots = {9, 9, 3, 9, 9, 3};
        for (int i = 0; i < packages.size(); i++) {
            ItemStackHandler contents = PackageItem.getContents(packages.get(i).stack);
            int used = 0;
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                used += contents.getStackInSlot(slot).isEmpty() ? 0 : 1;
            }
            helper.assertTrue(used == expectedSlots[i], "Package " + i + " has " + used + " slots, expected " + expectedSlots[i]);
        }
        helper.succeed();
    }

    /** Patterns with 9 slots must keep Create's own packaging (slot i = pattern slot i). */
    @GameTest(template = TestCrafters.ARENA)
    public static void repackagerKeepsCreateBehaviourFor3x3(GameTestHelper helper) {
        List<BigItemStack> pattern = new ArrayList<>();
        for (Item item : NINE) {
            pattern.add(new BigItemStack(new ItemStack(item), 1));
        }
        pattern.set(4, new BigItemStack(ItemStack.EMPTY, 1));
        InventorySummary summary = new InventorySummary();
        for (Item item : NINE) {
            summary.add(new ItemStack(item), 5);
        }
        List<BigItemStack> packages = new ExposedRepackageHelper().repack(summary, craftOrder(pattern, 2));
        helper.assertTrue(packages.size() == 1 && packages.get(0).count == 2, "Expected Create's single package ×2");
        ItemStackHandler contents = PackageItem.getContents(packages.get(0).stack);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack expected = pattern.get(slot).stack;
            helper.assertTrue(expected.isEmpty() ? contents.getStackInSlot(slot).isEmpty() : contents.getStackInSlot(slot).is(expected.getItem()),
                    "Slot " + slot + " differs from Create's layout");
        }
        helper.succeed();
    }

    /**
     * T4 end to end on the server side: a 3×3 recipe with nine different items is placed into a real 21-crafter group,
     * turned into a pattern, packed, and unpacked by Create's own crafter unpacking handler. Every item must end up in
     * the crafter at its recipe position.
     */
    @GameTest(template = TestCrafters.ARENA)
    public static void unpacksRecipeIntoRightCrafters(GameTestHelper helper) {
        List<String> layout = ProfileCodecGameTests.LAYOUT_21;
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(layout));
        CrafterRegion region = new CrafterRegion("21", group.controller(), group.facing(), RegionScanner.layoutOf(group, 9, 9).getOrThrow(), "");

        List<Ingredient> cells = new ArrayList<>();
        List<BigItemStack> inputs = new ArrayList<>();
        for (Item item : NINE) {
            cells.add(Ingredient.of(item));
            inputs.add(new BigItemStack(new ItemStack(item), 1));
        }
        List<Ingredient> template = RegionPlacer.templateFor(new GridRecipe(3, 3, cells, false), region).orElseThrow();
        List<BigItemStack> pattern = PatternBuilder.build(template, inputs, true);
        helper.assertTrue(pattern.size() == 21 && pattern.stream().filter(b -> !b.stack.isEmpty()).count() == 9,
                "Pattern must contain the nine items: " + pattern.size());
        helper.assertTrue(inputs.stream().allMatch(i -> i.count == 1), "PatternBuilder must not change the ordered amounts");

        List<List<ItemStack>> parts = LargePatternPackaging.splitByItem(pattern);
        helper.assertTrue(parts.size() == 1, "Nine items must fit into one package");
        boolean unpacked = CrafterUnpackingHandler.INSTANCE.unpack(helper.getLevel(), group.controller(), helper.getLevel().getBlockState(group.controller()),
                group.facing(), contentsOf(parts.get(0)), PackageOrderWithCrafts.singleRecipe(pattern), false);
        helper.assertTrue(unpacked, "Create's unpacking handler rejected the package");

        // The recipe sits at columns 1-3, rows 0-2 (corner (0,0) is a gap). Check each crafter against its grid cell.
        List<RegionScanner.Cell> gridCells = RegionScanner.cellsOf(group);
        for (int i = 0; i < group.size(); i++) {
            RegionScanner.Cell cell = gridCells.get(i);
            int rx = cell.column() - 1;
            int ry = cell.row();
            ItemStack inCrafter = crafterAt(helper, group.members().get(i)).getInventory().getStackInSlot(0);
            boolean inside = rx >= 0 && rx < 3 && ry >= 0 && ry < 3;
            if (inside) {
                Item expected = NINE[ry * 3 + rx];
                helper.assertTrue(inCrafter.is(expected), "Crafter at cell " + cell + " has " + inCrafter + ", expected " + expected);
            } else {
                helper.assertTrue(inCrafter.isEmpty(), "Crafter at cell " + cell + " should be empty but has " + inCrafter);
            }
        }
        helper.succeed();
    }

    /** T4 with a craft split over three packages: each package is accepted, and only the last completes the grid. */
    @GameTest(template = TestCrafters.ARENA)
    public static void splitPackagesFillGridBeforeStart(GameTestHelper helper) {
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_21));
        List<BigItemStack> pattern = pattern(Items.WATER_BUCKET, 21);
        List<List<ItemStack>> parts = LargePatternPackaging.splitByItem(pattern);
        helper.assertTrue(parts.size() == 3, "Expected 3 packages for 21 unstackable items");

        PackageOrderWithCrafts context = PackageOrderWithCrafts.singleRecipe(pattern);
        MechanicalCrafterBlockEntity controller = crafterAt(helper, group.controller());
        List<? extends IItemHandler> inventories = controller.getInput().getInventories(helper.getLevel(), group.controller());
        for (int p = 0; p < parts.size(); p++) {
            boolean unpacked = CrafterUnpackingHandler.INSTANCE.unpack(helper.getLevel(), group.controller(),
                    helper.getLevel().getBlockState(group.controller()), group.facing(), contentsOf(parts.get(p)), context, false);
            helper.assertTrue(unpacked, "Package " + (p + 1) + " was rejected");
            boolean complete = LargePatternPackaging.allPatternSlotsFilled(pattern, inventories);
            helper.assertTrue(complete == (p == parts.size() - 1), "Grid complete after package " + (p + 1) + ": " + complete);
        }
        helper.succeed();
    }

    /**
     * Regression test for the first in-game test: Create's {@code StockKeeperRequestScreen.sendIt} counts crafts on the
     * ordered list right after building the pattern. If the pattern builder reduced that list, Create found 0 crafts and
     * sent the order without crafting information; the crafters were then filled one after another (all andesite alloy
     * first, then planks, ...). This replays Create 6.0.10's counting loop with our pattern builder.
     */
    @GameTest(template = TestCrafters.ARENA)
    public static void orderKeepsCraftingEntry(GameTestHelper helper) {
        CrafterRegion region = new CrafterRegion("21", BlockPos.ZERO, Direction.NORTH, ProfileCodecGameTests.LAYOUT_21, "");
        // door-like recipe: 2 wide, 3 high, one item type
        List<Ingredient> door = Collections.nCopies(6, Ingredient.of(Items.OAK_PLANKS));
        List<Ingredient> template = RegionPlacer.templateFor(new GridRecipe(2, 3, door, false), region).orElseThrow();
        for (int crafts : new int[] {1, 3}) {
            List<BigItemStack> itemsToOrder = List.of(new BigItemStack(new ItemStack(Items.OAK_PLANKS), 6 * crafts));
            List<PackageOrderWithCrafts.CraftingEntry> entries = createSendItCraftList(template, itemsToOrder, crafts);
            int total = entries.stream().mapToInt(PackageOrderWithCrafts.CraftingEntry::count).sum();
            helper.assertTrue(total == crafts, crafts + " craft(s) ordered, but the crafting list has " + total);
            helper.assertTrue(entries.get(0).pattern().stacks().size() == 21, "Pattern must have one slot per crafter");
        }
        helper.succeed();
    }

    /** Create 6.0.10 {@code StockKeeperRequestScreen.sendIt}, crafting part, with our pattern builder in place of Create's 3×3 one. */
    private static List<PackageOrderWithCrafts.CraftingEntry> createSendItCraftList(List<Ingredient> template, List<BigItemStack> itemsToOrder,
                                                                                  int targetCount) {
        List<PackageOrderWithCrafts.CraftingEntry> craftList = new ArrayList<>();
        int craftedCount = 0;
        List<BigItemStack> mutableOrder = BigItemStack.duplicateWrappers(itemsToOrder);
        while (craftedCount < targetCount) {
            PackageOrder pattern = new PackageOrder(PatternBuilder.build(template, mutableOrder, true));
            int maxCrafts = targetCount - craftedCount;
            int availableCrafts = 0;
            boolean itemsExhausted = false;
            outer:
            while (availableCrafts < maxCrafts && !itemsExhausted) {
                List<BigItemStack> previousSnapshot = BigItemStack.duplicateWrappers(mutableOrder);
                itemsExhausted = true;
                patternLoop:
                for (BigItemStack patternStack : pattern.stacks()) {
                    if (patternStack.stack.isEmpty()) {
                        continue;
                    }
                    for (BigItemStack ordered : mutableOrder) {
                        if (ItemStack.isSameItemSameComponents(ordered.stack, patternStack.stack) && ordered.count != 0) {
                            ordered.count--;
                            itemsExhausted = false;
                            continue patternLoop;
                        }
                    }
                    mutableOrder = previousSnapshot;
                    break outer;
                }
                availableCrafts++;
            }
            if (availableCrafts == 0) {
                break;
            }
            craftList.add(new PackageOrderWithCrafts.CraftingEntry(pattern, availableCrafts));
            craftedCount += availableCrafts;
        }
        return craftList;
    }

    private static MechanicalCrafterBlockEntity crafterAt(GameTestHelper helper, BlockPos pos) {
        MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(helper.getLevel(), pos);
        if (crafter == null) {
            throw new IllegalStateException("No crafter at " + pos);
        }
        return crafter;
    }

    /** Fresh copies of a package's items, as Create's unpacking handler receives (and consumes) them. */
    private static List<ItemStack> contentsOf(List<ItemStack> part) {
        return part.stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));
    }
}
