package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Builds a Create recipe pattern (one {@link BigItemStack} per crafter) from a {@link Placement#template()}.
 * Same contract as Create's {@code FactoryPanelScreen.convertRecipeToPackageOrderContext}: each slot gets the first
 * ordered item matching its ingredient (count 1, or empty), and with {@code respectAmounts} the chosen item's count in
 * {@code inputs} is reduced by one.
 */
public final class PatternBuilder {
    private PatternBuilder() {
    }

    public static List<BigItemStack> build(List<Ingredient> template, List<BigItemStack> inputs, boolean respectAmounts) {
        List<BigItemStack> pattern = new ArrayList<>(template.size());
        BigItemStack empty = new BigItemStack(ItemStack.EMPTY, 1);
        for (Ingredient ingredient : template) {
            BigItemStack slot = empty;
            if (!ingredient.isEmpty()) {
                for (BigItemStack input : inputs) {
                    if (input.count > 0 && ingredient.test(input.stack)) {
                        slot = new BigItemStack(input.stack, 1);
                        if (respectAmounts) {
                            input.count--;
                        }
                        break;
                    }
                }
            }
            pattern.add(slot);
        }
        return pattern;
    }
}
