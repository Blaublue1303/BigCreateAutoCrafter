package com.blaublue1303.bigcreateautocrafter.order;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Server-side packaging for recipe patterns with more than 9 slots (larger crafter regions).
 *
 * <p>Create packs one package per craft and puts pattern slot {@code i} into package slot {@code i}, which cuts patterns
 * off after 9 slots (a package always has {@link PackageItem#SLOTS} slots). Create's crafter unpacking handler, however,
 * matches package items to pattern slots by item, not by package slot. Large patterns are therefore packed grouped by item
 * type (up to a full stack per package slot). If a craft still needs more than 9 slots, it is split into several packages
 * that all carry the full pattern, interleaved per craft; the crafter only starts once all pattern slots are filled
 * ({@link #shouldStartCrafting}).
 *
 * <p>Patterns with at most 9 slots (all of Create's own orders) are left to Create.
 */
public final class LargePatternPackaging {
    private LargePatternPackaging() {
    }

    public static boolean isLarge(List<BigItemStack> pattern) {
        return pattern.size() > PackageItem.SLOTS;
    }

    /**
     * Packages for {@code crafts} crafts of a large pattern; replaces the single cut-off package Create builds per
     * recipe entry. Create has already counted the crafts the collected items allow.
     */
    public static List<BigItemStack> packagesFor(List<BigItemStack> pattern, int crafts, RandomSource random) {
        PackageOrderWithCrafts context = PackageOrderWithCrafts.singleRecipe(pattern);
        List<ItemStack> boxes = splitByItem(pattern).stream().map(part -> box(part, context, random)).toList();
        if (boxes.size() == 1) {
            return List.of(new BigItemStack(boxes.get(0), crafts));
        }
        // The packager copies each queued stack when sending it, so the boxes can be shared between crafts.
        List<BigItemStack> packages = new ArrayList<>(crafts * boxes.size());
        for (int craft = 0; craft < crafts; craft++) {
            for (ItemStack box : boxes) {
                packages.add(new BigItemStack(box, 1));
            }
        }
        return packages;
    }

    /** The pattern's items grouped by type, up to a full stack per slot, cut into packages of {@link PackageItem#SLOTS} slots. */
    public static List<List<ItemStack>> splitByItem(List<BigItemStack> pattern) {
        List<ItemStack> slots = new ArrayList<>();
        for (BigItemStack slot : pattern) {
            if (!slot.stack.isEmpty()) {
                ItemHelper.addToList(slot.stack.copyWithCount(1), slots);
            }
        }
        return Lists.partition(slots, PackageItem.SLOTS);
    }

    /**
     * Guard for Create's {@code CrafterUnpackingHandler}: after a package was unpacked into a crafter group, Create
     * starts crafting immediately (empty crafters allowed). For large patterns that arrive in several packages this
     * would start with an incomplete grid, so crafting only starts once every pattern slot has its item.
     */
    public static boolean shouldStartCrafting(Level level, BlockPos pos, MechanicalCrafterBlockEntity crafter, PackageOrderWithCrafts orderContext) {
        if (!PackageOrderWithCrafts.hasCraftingInformation(orderContext)) {
            return true;
        }
        List<BigItemStack> pattern = orderContext.getCraftingInformation();
        return !isLarge(pattern) || allPatternSlotsFilled(pattern, crafter.getInput().getInventories(level, pos));
    }

    public static boolean allPatternSlotsFilled(List<BigItemStack> pattern, List<? extends IItemHandler> inventories) {
        int max = Math.min(pattern.size(), inventories.size());
        for (int i = 0; i < max; i++) {
            if (!pattern.get(i).stack.isEmpty() && inventories.get(i).getStackInSlot(0).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack box(List<ItemStack> contents, PackageOrderWithCrafts context, RandomSource random) {
        ItemStack box = PackageItem.containing(contents);
        PackageItem.setOrder(box, random.nextInt(), 0, true, 0, true, context);
        return box;
    }
}
