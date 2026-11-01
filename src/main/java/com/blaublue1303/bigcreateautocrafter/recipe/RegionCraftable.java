package com.blaublue1303.bigcreateautocrafter.recipe;

import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/**
 * A Stock Keeper order entry added through a crafter profile: Create's {@link CraftableBigItemStack} plus the region the
 * recipe was placed in. It lives exactly as long as Create keeps the entry in its order list.
 */
public class RegionCraftable extends CraftableBigItemStack {
    public final Placement placement;

    public RegionCraftable(ItemStack stack, Recipe<?> recipe, Placement placement) {
        super(stack, recipe);
        this.placement = placement;
    }
}
