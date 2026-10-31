package com.blaublue1303.bigcreateautocrafter.order;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Server-side packaging for recipe patterns with more than 9 slots (larger crafter regions).
 *
 * <p>Create packs one package per craft and puts pattern slot {@code i} into package slot {@code i}, which cuts patterns
 * off after 9 slots (a package always has 9 slots). Create's crafter unpacking handler, however, matches package
 * items to pattern slots by item, not by package slot. Large patterns are therefore packed grouped by item type (up to
 * a full stack per package slot). If a craft still needs more than 9 slots, it is split into several packages that all
 * carry the full pattern, interleaved per craft; the crafter only starts once all pattern slots are filled
 * ({@link #shouldStartCrafting}).
 *
 * <p>Patterns with at most 9 slots (all of Create's own orders) keep Create's exact behaviour.
 */
public final class LargePatternPackaging {
    public static final int PACKAGE_SLOTS = 9;

    private LargePatternPackaging() {
    }

    public static boolean hasLargePattern(PackageOrderWithCrafts order) {
        return order.orderedCrafts().stream().anyMatch(e -> e.pattern().stacks().size() > PACKAGE_SLOTS);
    }

    /** Replacement for {@code PackageRepackageHelper.repackBasedOnRecipes} when {@link #hasLargePattern} is true. */
    public static List<BigItemStack> repackBasedOnRecipes(InventorySummary summary, PackageOrderWithCrafts order, RandomSource random) {
        List<BigItemStack> packages = new ArrayList<>();
        for (CraftingEntry entry : order.orderedCrafts()) {
            List<BigItemStack> pattern = entry.pattern().stacks();
            int crafts = takeCrafts(summary, entry);
            PackageOrderWithCrafts context = PackageOrderWithCrafts.singleRecipe(pattern);

            if (pattern.size() <= PACKAGE_SLOTS) {
                // Create's own behaviour: pattern slot i -> package slot i
                ItemStackHandler target = new ItemStackHandler(PACKAGE_SLOTS);
                for (int slot = 0; slot < pattern.size(); slot++) {
                    target.setStackInSlot(slot, pattern.get(slot).stack.copyWithCount(1));
                }
                packages.add(new BigItemStack(box(target, context, random), crafts));
                continue;
            }

            List<ItemStackHandler> parts = splitByItem(pattern);
            if (parts.size() == 1) {
                packages.add(new BigItemStack(box(parts.get(0), context, random), crafts));
            } else {
                for (int craft = 0; craft < crafts; craft++) {
                    for (ItemStackHandler part : parts) {
                        packages.add(new BigItemStack(box(copy(part), context, random), 1));
                    }
                }
            }
        }
        return packages;
    }

    /** Same counting as Create: reduce {@code summary} by one pattern per craft while items last. */
    private static int takeCrafts(InventorySummary summary, CraftingEntry entry) {
        int crafts = 0;
        for (int i = 0; i < entry.count(); i++) {
            for (BigItemStack required : entry.pattern().stacks()) {
                if (required.stack.isEmpty()) {
                    continue;
                }
                if (summary.getCountOf(required.stack) <= 0) {
                    return crafts;
                }
                summary.add(required.stack, -1);
            }
            crafts++;
        }
        return crafts;
    }

    /** Groups the pattern's items by type and fills package slots with up to a full stack each. */
    public static List<ItemStackHandler> splitByItem(List<BigItemStack> pattern) {
        List<ItemStack> totals = new ArrayList<>();
        for (BigItemStack slot : pattern) {
            if (slot.stack.isEmpty()) {
                continue;
            }
            ItemStack total = null;
            for (ItemStack existing : totals) {
                if (ItemStack.isSameItemSameComponents(existing, slot.stack)) {
                    total = existing;
                    break;
                }
            }
            if (total == null) {
                totals.add(slot.stack.copyWithCount(1));
            } else {
                total.grow(1);
            }
        }

        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack total : totals) {
            int remaining = total.getCount();
            int max = Math.max(1, total.getMaxStackSize());
            while (remaining > 0) {
                int count = Math.min(remaining, max);
                slots.add(total.copyWithCount(count));
                remaining -= count;
            }
        }

        List<ItemStackHandler> parts = new ArrayList<>();
        for (int start = 0; start < slots.size(); start += PACKAGE_SLOTS) {
            ItemStackHandler part = new ItemStackHandler(PACKAGE_SLOTS);
            for (int i = start; i < Math.min(slots.size(), start + PACKAGE_SLOTS); i++) {
                part.setStackInSlot(i - start, slots.get(i));
            }
            parts.add(part);
        }
        return parts;
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
        if (pattern.size() <= PACKAGE_SLOTS) {
            return true;
        }
        return allPatternSlotsFilled(pattern, crafter.getInput().getInventories(level, pos));
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

    private static ItemStack box(ItemStackHandler contents, PackageOrderWithCrafts context, RandomSource random) {
        ItemStack box = PackageItem.containing(contents);
        PackageItem.setOrder(box, random.nextInt(), 0, true, 0, true, context);
        return box;
    }

    private static ItemStackHandler copy(ItemStackHandler source) {
        ItemStackHandler copy = new ItemStackHandler(source.getSlots());
        for (int i = 0; i < source.getSlots(); i++) {
            copy.setStackInSlot(i, source.getStackInSlot(i).copy());
        }
        return copy;
    }
}
