package com.blaublue1303.bigcreateautocrafter.order;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.blaublue1303.bigcreateautocrafter.profile.RegionOwners;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One crafting order whose ingredients come from several packagers (no Repackager in between).
 *
 * <p>Create's {@code LogisticsManager} puts the recipe only on the packages of the first packager of an order; the
 * packages of the other packagers arrive without it, and Create's crafter unpacking then fills their items into the
 * crafters one after another, i.e. into the wrong crafters. Which packager comes first depends on where the items are
 * stored, so this can start "after a while" when the stock moves. All packages of one order share Create's order id,
 * so the recipe of the first package is remembered per crafter group and order id and used for the others:
 * <ul>
 *   <li>A package with recipe is unpacked as usual and its recipe remembered.</li>
 *   <li>A package without recipe whose order has a remembered recipe is unpacked with that recipe.</li>
 *   <li>A package without recipe and no remembered recipe waits (stays in the packager) if the crafters are a
 *       registered region, at most {@value #MAX_WAIT_TICKS} ticks, then Create's own behaviour is used. Other crafter
 *       groups keep Create's behaviour right away.</li>
 * </ul>
 */
public final class SplitOrderUnpacking {
    /** How long a package without recipe waits at a registered region for the package of its order with the recipe. */
    public static final long MAX_WAIT_TICKS = 20 * 60 * 5;
    /** Waiting is reported to the owner after this long (short waits are normal: packages travel differently). */
    static final long WARN_AFTER_TICKS = 20 * 20;
    /** A remembered recipe is dropped this long after its last use. */
    private static final long RECIPE_TTL_TICKS = 20 * 60 * 10;

    /** Create's unpacking with the given recipe context. */
    @FunctionalInterface
    public interface Unpacker {
        boolean unpack(@Nullable PackageOrderWithCrafts context);
    }

    private record Key(ResourceKey<Level> dimension, BlockPos controller, int orderId) {
    }

    private record Remembered(PackageOrderWithCrafts context, long lastUse) {
    }

    private static final Map<Key, Remembered> RECIPES = new ConcurrentHashMap<>();
    private static final Map<Key, Long> WAITING_SINCE = new ConcurrentHashMap<>();

    private SplitOrderUnpacking() {
    }

    /**
     * Unpacks {@code box} into the block at {@code target}.
     *
     * @param crafterTarget whether Create's crafter unpacking handler is used for the target
     * @param create        Create's unpacking, called with the recipe context to use
     */
    public static boolean unpack(Level level, BlockPos target, ItemStack box, @Nullable PackageOrderWithCrafts context, boolean simulate,
                                 boolean crafterTarget, Unpacker create) {
        if (!(level instanceof ServerLevel serverLevel) || !crafterTarget || !PackageItem.hasOrderData(box)) {
            return create.unpack(context);
        }
        Optional<BlockPos> controller = CreateCompat.controllerPos(level, target);
        if (controller.isEmpty()) {
            return create.unpack(context);
        }
        long now = level.getGameTime();
        expire(now);
        Key key = new Key(level.dimension(), controller.get().immutable(), PackageItem.getOrderId(box));

        if (PackageOrderWithCrafts.hasCraftingInformation(context)) {
            boolean unpacked = create.unpack(context);
            if (unpacked && !simulate) {
                RECIPES.put(key, new Remembered(context, now));
            }
            return unpacked;
        }

        Remembered remembered = RECIPES.get(key);
        if (remembered != null) {
            boolean unpacked = create.unpack(remembered.context());
            if (unpacked && !simulate) {
                RECIPES.put(key, new Remembered(remembered.context(), now));
                WAITING_SINCE.remove(key);
            }
            return unpacked;
        }

        if (RegionOwners.of(serverLevel, controller.get()).isEmpty()) {
            return create.unpack(context); // a crafter group nobody registered: Create's behaviour
        }
        long since = WAITING_SINCE.computeIfAbsent(key, k -> now);
        if (now - since < MAX_WAIT_TICKS) {
            if (now - since >= WARN_AFTER_TICKS) {
                DeliveryWarnings.waitingForRecipe(serverLevel, controller.get());
            }
            return false;
        }
        DeliveryWarnings.gaveUp(serverLevel, controller.get());
        return create.unpack(context);
    }

    /** Whether a recipe is remembered for this order at the group with this controller (tests). */
    public static boolean remembers(Level level, BlockPos controller, int orderId) {
        return RECIPES.containsKey(new Key(level.dimension(), controller, orderId));
    }

    private static void expire(long now) {
        // game time can also jump back (another world in single player): drop such entries as well
        RECIPES.values().removeIf(r -> now < r.lastUse() || now - r.lastUse() > RECIPE_TTL_TICKS);
        WAITING_SINCE.values().removeIf(since -> now < since || now - since > MAX_WAIT_TICKS + RECIPE_TTL_TICKS);
    }

    public static void clear() {
        RECIPES.clear();
        WAITING_SINCE.clear();
    }
}
