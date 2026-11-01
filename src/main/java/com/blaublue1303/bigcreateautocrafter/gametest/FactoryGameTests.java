package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.List;
import java.util.Optional;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.recipe.FactoryArrangements;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Factory Gauge on a crafter region: recipe search, arrangement and address lookup, with Create's real recipes. */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class FactoryGameTests {
    private static final List<BigItemStack> WHEEL_INPUTS = List.of(
            new BigItemStack(new ItemStack(AllItems.ANDESITE_ALLOY.get()), 16),
            new BigItemStack(new ItemStack(Items.OAK_PLANKS), 4),
            new BigItemStack(new ItemStack(Items.STONE), 1));

    private static CraftingRecipe recipe(GameTestHelper helper, ResourceLocation id) {
        return (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void findsMechanicalRecipeForGaugeInputs(GameTestHelper helper) {
        CrafterRegion region21 = ProfileCodecGameTests.sampleProfile().regions().get(0);
        ItemStack wheel = new ItemStack(AllBlocks.CRUSHING_WHEEL.get());
        Optional<CraftingRecipe> found = FactoryArrangements.findMechanicalRecipe(helper.getLevel(), wheel, WHEEL_INPUTS,
                r -> FactoryArrangements.forRegion(r, WHEEL_INPUTS, region21).isPresent());
        helper.assertTrue(found.isPresent() && !FactoryArrangements.fitsThreeByThree(found.get()), "Crushing wheel recipe not found");
        helper.assertTrue(FactoryArrangements.findMechanicalRecipe(helper.getLevel(), wheel, WHEEL_INPUTS, r -> false).isEmpty(),
                "Recipes no region can craft must not be offered");
        helper.assertTrue(FactoryArrangements.findMechanicalRecipe(helper.getLevel(), wheel, WHEEL_INPUTS.subList(0, 2), r -> true).isEmpty(),
                "Recipe with missing inputs must not be offered");
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void buildsRegionArrangement(GameTestHelper helper) {
        CrafterRegion region21 = ProfileCodecGameTests.sampleProfile().regions().get(0);
        CraftingRecipe wheel = recipe(helper, ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel"));
        List<ItemStack> arrangement = FactoryArrangements.forRegion(wheel, WHEEL_INPUTS, region21).orElseThrow();
        helper.assertTrue(arrangement.size() == 21 && arrangement.stream().noneMatch(ItemStack::isEmpty), "Wheel must fill all 21 crafters");
        helper.assertTrue(count(arrangement, AllItems.ANDESITE_ALLOY.get()) == 16 && count(arrangement, Items.OAK_PLANKS) == 4
                && count(arrangement, Items.STONE) == 1, "Wrong item amounts in the arrangement");

        CraftingRecipe chest = recipe(helper, ResourceLocation.withDefaultNamespace("chest"));
        List<BigItemStack> planks = List.of(new BigItemStack(new ItemStack(Items.OAK_PLANKS), 8));
        List<ItemStack> chestArrangement = FactoryArrangements.forRegion(chest, planks, region21).orElseThrow();
        helper.assertTrue(chestArrangement.size() == 21 && count(chestArrangement, Items.OAK_PLANKS) == 8,
                "Chest arrangement in the 21 region is wrong");
        helper.assertTrue(FactoryArrangements.fitsThreeByThree(chest), "Chest fits 3×3");
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void findsRegionByAddress(GameTestHelper helper) {
        List<CrafterProfile> profiles = List.of(ProfileCodecGameTests.sampleProfile()); // region 0 has address "crafter_gross"
        helper.assertTrue(FactoryArrangements.regionForAddress(profiles, Level.OVERWORLD, " crafter_gross ").map(CrafterRegion::name)
                .orElse("").equals("Groß"), "Region not found by address");
        helper.assertTrue(FactoryArrangements.regionForAddress(profiles, Level.OVERWORLD, "").isEmpty(), "Empty address must not match");
        helper.assertTrue(FactoryArrangements.regionForAddress(profiles, Level.NETHER, "crafter_gross").isEmpty(), "Other dimension must not match");
        helper.succeed();
    }

    /** The user's case: "Use Mechanical Crafting" for the wheel with a gauge address that is no region's address. */
    @GameTest(template = TestCrafters.ARENA)
    public static void choosesRegionForGauge(GameTestHelper helper) {
        List<CrafterProfile> profiles = List.of(ProfileCodecGameTests.sampleProfile()); // regions: 21 "crafter_gross", 3×3 ""
        CraftingRecipe wheel = recipe(helper, ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel"));
        for (String address : List.of("", "somewhere_else", "crafter_gross")) {
            FactoryArrangements.Target target = FactoryArrangements.target(profiles, Level.OVERWORLD, address, wheel, WHEEL_INPUTS).orElse(null);
            helper.assertTrue(target != null && target.region().name().equals("Groß"), "Wheel must go to the 21 region (address '" + address + "')");
            helper.assertTrue(target.countOf(new ItemStack(AllItems.ANDESITE_ALLOY.get())) == 16 && target.countOf(new ItemStack(Items.OAK_PLANKS)) == 4
                    && target.countOf(new ItemStack(Items.STONE)) == 1, "Wrong amounts per craft for the wheel");
        }
        helper.assertTrue(FactoryArrangements.target(profiles, Level.NETHER, "", wheel, WHEEL_INPUTS).isEmpty(),
                "Regions of another dimension must not be used");

        CraftingRecipe chest = recipe(helper, ResourceLocation.withDefaultNamespace("chest"));
        List<BigItemStack> planks = List.of(new BigItemStack(new ItemStack(Items.OAK_PLANKS), 8));
        helper.assertTrue(FactoryArrangements.target(profiles, Level.OVERWORLD, "somewhere_else", chest, planks).isEmpty(),
                "A 3×3 recipe to another address stays with Create");
        helper.assertTrue(FactoryArrangements.target(profiles, Level.OVERWORLD, "crafter_gross", chest, planks)
                .map(t -> t.region().name()).orElse("").equals("Groß"), "A 3×3 recipe to a region's address uses that region");
        helper.succeed();
    }

    private static long count(List<ItemStack> arrangement, Item item) {
        return arrangement.stream().filter(stack -> stack.is(item)).count();
    }
}
