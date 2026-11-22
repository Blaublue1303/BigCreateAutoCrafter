package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.order.SplitOrderUnpacking;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileManager;
import com.blaublue1303.bigcreateautocrafter.profile.RegionScanner;
import com.blaublue1303.bigcreateautocrafter.recipe.FactoryArrangements;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.crafter.CrafterHelper;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One crushing wheel ordered from two packagers without a Repackager: the alloy comes with the recipe (first
 * packager), planks and stone without it (second packager), both with the same order id.
 */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class SplitOrderGameTests {
    private static final List<BigItemStack> WHEEL_INPUTS = List.of(
            new BigItemStack(new ItemStack(AllItems.ANDESITE_ALLOY.get()), 16),
            new BigItemStack(new ItemStack(Items.OAK_PLANKS), 4),
            new BigItemStack(new ItemStack(Items.STONE), 1));

    /** The 21 group, its region and the crushing wheel pattern for it. */
    private record Setup(ConnectedCrafterGroup group, CrafterRegion region, List<ItemStack> pattern, PackageOrderWithCrafts recipe) {
        BlockPos target() {
            return group.controller();
        }
    }

    private static Setup setUp(GameTestHelper helper) {
        ConnectedCrafterGroup group = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_21));
        CrafterRegion region = new CrafterRegion("21", group.controller(), group.facing(), RegionScanner.layoutOf(group, 9, 9).getOrThrow(), "");
        CraftingRecipe wheel = (CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel")).orElseThrow().value();
        List<ItemStack> pattern = FactoryArrangements.forRegion(wheel, WHEEL_INPUTS, region).orElseThrow();
        return new Setup(group, region, pattern, PackageOrderWithCrafts.singleRecipe(pattern.stream().map(BigItemStack::new).toList()));
    }

    /** A package of order {@code orderId} as packager {@code link} sends it: only link 0 carries the recipe. */
    private static ItemStack box(int orderId, int link, PackageOrderWithCrafts recipe, ItemStack... contents) {
        ItemStack box = PackageItem.containing(List.of(contents));
        PackageItem.setOrder(box, orderId, link, link == 1, 0, true, link == 0 ? recipe : null);
        return box;
    }

    private static boolean unpack(GameTestHelper helper, Setup setup, ItemStack box) {
        // like the packager: the non-empty stacks of the package, as copies
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < PackageItem.SLOTS; slot++) {
            ItemStack stack = PackageItem.getContents(box).getStackInSlot(slot);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        BlockPos target = setup.target();
        return SplitOrderUnpacking.unpack(helper.getLevel(), target, box, PackageItem.getOrderContext(box), false, true,
                context -> CrafterUnpackingHandler.INSTANCE.unpack(helper.getLevel(), target, helper.getLevel().getBlockState(target),
                        Direction.NORTH, items, context, false));
    }

    private static void assertCraftersHoldPattern(GameTestHelper helper, Setup setup) {
        for (int i = 0; i < setup.group().size(); i++) {
            ItemStack inCrafter = CrafterHelper.getCrafter(helper.getLevel(), setup.group().members().get(i)).getInventory().getStackInSlot(0);
            helper.assertTrue(ItemStack.isSameItem(inCrafter, setup.pattern().get(i)),
                    "Crafter " + i + " holds " + inCrafter + ", expected " + setup.pattern().get(i));
        }
    }

    private static ItemStack alloy() {
        return new ItemStack(AllItems.ANDESITE_ALLOY.get(), 16);
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void packageWithoutRecipeUsesRememberedRecipe(GameTestHelper helper) {
        Setup setup = setUp(helper);
        helper.assertTrue(unpack(helper, setup, box(101, 0, setup.recipe(), alloy())), "Package with recipe must be unpacked");
        helper.assertTrue(SplitOrderUnpacking.remembers(helper.getLevel(), setup.group().controller(), 101), "Recipe must be remembered");
        helper.assertTrue(unpack(helper, setup, box(101, 1, setup.recipe(), new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.STONE))),
                "Package without recipe must be unpacked with the remembered recipe");
        assertCraftersHoldPattern(helper, setup);
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void registeredRegionWaitsForRecipe(GameTestHelper helper) {
        Setup setup = setUp(helper);
        ProfileManager profiles = ProfileManager.get(helper.getLevel().getServer());
        CrafterProfile profile = new CrafterProfile(UUID.randomUUID(), "Split", helper.getLevel().dimension(), UUID.randomUUID(),
                List.of(setup.region()));
        profiles.put(profile);
        try {
            ItemStack withoutRecipe = box(102, 1, setup.recipe(), new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.STONE));
            helper.assertFalse(unpack(helper, setup, withoutRecipe), "Package without recipe must wait while the recipe is unknown");
            helper.assertTrue(CrafterHelper.getCrafter(helper.getLevel(), setup.group().members().get(0)).getInventory().getStackInSlot(0).isEmpty(),
                    "Nothing may be put into the crafters while waiting");
            helper.assertTrue(unpack(helper, setup, box(102, 0, setup.recipe(), alloy())), "Package with recipe must be unpacked");
            helper.assertTrue(unpack(helper, setup, withoutRecipe), "The waiting package must go in once the recipe is known");
            assertCraftersHoldPattern(helper, setup);
        } finally {
            profiles.remove(profile.id());
        }
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void unregisteredGroupKeepsCreateBehaviour(GameTestHelper helper) {
        Setup setup = setUp(helper);
        helper.assertTrue(unpack(helper, setup, box(103, 1, setup.recipe(), new ItemStack(Items.OAK_PLANKS, 4))),
                "A group nobody registered must unpack like Create, without waiting");
        helper.succeed();
    }

    /** The same through a real packager: the mixin on {@code unwrapBox} must apply. */
    @GameTest(template = TestCrafters.ARENA)
    public static void packagerSharesRecipeWithinOrder(GameTestHelper helper) {
        Setup setup = setUp(helper);
        // packager right in front of the bottom-middle crafter (relative 3,1,3), facing away from it
        BlockPos packagerRel = new BlockPos(3, 1, 2);
        helper.setBlock(packagerRel, AllBlocks.PACKAGER.getDefaultState().setValue(PackagerBlock.FACING, Direction.NORTH));
        PackagerBlockEntity packager = (PackagerBlockEntity) helper.getBlockEntity(packagerRel);
        helper.assertTrue(packager.unwrapBox(box(104, 0, setup.recipe(), alloy()), false), "Packager must unpack the package with recipe");
        helper.assertTrue(SplitOrderUnpacking.remembers(helper.getLevel(), setup.group().controller(), 104),
                "Unpacking through the packager must remember the recipe");
        // the packager animates for 20 ticks before it takes the next package
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(packager.unwrapBox(box(104, 1, setup.recipe(), new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.STONE)), false),
                    "Packager must unpack the package without recipe");
            assertCraftersHoldPattern(helper, setup);
            helper.succeed();
        });
    }
}
