package com.blaublue1303.bigcreateautocrafter.recipe;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.BigItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Builds a Create recipe pattern (one {@link BigItemStack} per crafter) from a {@link Placement#template()}.
 *
 * <p>Same contract as Create's {@code FactoryPanelScreen.convertRecipeToPackageOrderContext}: each slot gets the first
 * ordered item matching its ingredient (count 1, or empty). With {@code respectAmounts}, amounts are counted down on a
 * <b>copy</b> of {@code inputs}, so one pattern does not use more of an item than was ordered. The caller's list is never
 * changed: Create's {@code StockKeeperRequestScreen.sendIt} counts the crafts on that list right after building the
 * pattern, and a reduced list there means "0 crafts", which sends the order without crafting information.
 */
public final class PatternBuilder {
    private PatternBuilder() {
    }

    public static List<BigItemStack> build(List<Ingredient> template, List<BigItemStack> inputs, boolean respectAmounts) {
        List<BigItemStack> available = BigItemStack.duplicateWrappers(inputs);
        List<BigItemStack> pattern = new ArrayList<>(template.size());
        BigItemStack empty = new BigItemStack(ItemStack.EMPTY, 1);
        for (Ingredient ingredient : template) {
            BigItemStack slot = empty;
            if (!ingredient.isEmpty()) {
                for (BigItemStack input : available) {
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
