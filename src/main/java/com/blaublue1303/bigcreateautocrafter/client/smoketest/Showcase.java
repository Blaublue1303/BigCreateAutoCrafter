package com.blaublue1303.bigcreateautocrafter.client.smoketest;

import java.util.ArrayList;
import java.util.List;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileManager;
import com.blaublue1303.bigcreateautocrafter.recipe.FactoryArrangements;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.impl.unpacking.CrafterUnpackingHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameRules;

/**
 * Pictures for the project page, taken during the client smoke test: the crafter setup in the world with a crushing
 * wheel loaded through Create's own unpacking, and large renders of the items for the project logo.
 */
final class Showcase {
    /** Key colour behind the item renders; the logo script makes it transparent. */
    static final int KEY_COLOR = 0xFFFF00FF;

    private Showcase() {
    }

    /**
     * Noon, the crushing wheel's ingredients unpacked into the 21 region like a package from a Factory Gauge would be,
     * and the player a few blocks back so both regions are in view.
     */
    static void stageWorld(ServerPlayer player, BlockPos origin) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        CrafterRegion region = ProfileManager.get(level.getServer()).byOwner(player.getUUID()).get(0).regions().get(0);
        CraftingRecipe wheel = (CraftingRecipe) level.getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting/crushing_wheel")).orElseThrow().value();
        List<BigItemStack> inputs = List.of(new BigItemStack(AllItems.ANDESITE_ALLOY.asStack(), 16),
                new BigItemStack(new ItemStack(Items.OAK_PLANKS), 4), new BigItemStack(new ItemStack(Items.STONE), 1));
        List<ItemStack> arrangement = FactoryArrangements.forRegion(wheel, inputs, region).orElseThrow();
        List<ItemStack> contents = new ArrayList<>();
        arrangement.forEach(stack -> ItemHelper.addToList(stack.copy(), contents));
        boolean unpacked = CrafterUnpackingHandler.INSTANCE.unpack(level, region.controller(), level.getBlockState(region.controller()),
                region.facing(), contents, PackageOrderWithCrafts.singleRecipe(arrangement.stream().map(BigItemStack::new).toList()), false);
        BigCreateAutoCrafter.LOGGER.info("[smoke] showcase: crushing wheel unpacked into the 21 region: {}", unpacked);
        player.teleportTo(level, origin.getX() - 1.5, origin.getY(), origin.getZ() + 4.5, 180, -10);
    }

    static void returnPlayer(ServerPlayer player, BlockPos origin) {
        player.teleportTo(player.serverLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 180, 0);
    }

    /** The Mechanical Crafter and the crushing wheel as inventory items, each in a 256 × 256 GUI unit square on the key colour. */
    static final class ItemRenders extends Screen {
        private static final int SCALE = 16;

        ItemRenders() {
            super(Component.literal("Item renders"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, KEY_COLOR);
            renderLarge(graphics, AllBlocks.MECHANICAL_CRAFTER.asStack(), 0);
            renderLarge(graphics, AllBlocks.CRUSHING_WHEEL.asStack(), 16 * SCALE + 8);
        }

        private void renderLarge(GuiGraphics graphics, ItemStack stack, int x) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, 0, 0);
            graphics.pose().scale(SCALE, SCALE, SCALE);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}
