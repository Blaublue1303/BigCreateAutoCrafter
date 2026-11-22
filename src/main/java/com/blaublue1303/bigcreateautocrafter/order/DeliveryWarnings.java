package com.blaublue1303.bigcreateautocrafter.order;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.blaublue1303.bigcreateautocrafter.profile.RegionOwners;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Tells the owner of a crafter region (and the server log) when a delivery cannot be put into the crafters as ordered,
 * instead of letting it wait or go wrong silently. At most once a minute per crafter group and kind of problem.
 */
public final class DeliveryWarnings {
    private static final String MSG = "message." + BigCreateAutoCrafter.MODID + ".";
    private static final long INTERVAL_TICKS = 20 * 60;

    enum Kind {
        /** A package without recipe waits for the package of the same order that carries it. */
        WAITING_FOR_RECIPE("delivery_waiting", ChatFormatting.YELLOW),
        /** No package with the recipe came in time; Create's own order-less filling was used. */
        GAVE_UP("delivery_gave_up", ChatFormatting.RED),
        /** The controller lists a crafter Create cannot reach; the package waits. */
        GROUP_INCOMPLETE("delivery_incomplete", ChatFormatting.RED);

        private final String key;
        private final ChatFormatting colour;

        Kind(String key, ChatFormatting colour) {
            this.key = key;
            this.colour = colour;
        }
    }

    private record Key(ResourceKey<Level> dimension, BlockPos controller, Kind kind) {
    }

    private static final Map<Key, Long> LAST = new ConcurrentHashMap<>();

    private DeliveryWarnings() {
    }

    public static void waitingForRecipe(ServerLevel level, BlockPos controller) {
        report(level, controller, Kind.WAITING_FOR_RECIPE);
    }

    public static void gaveUp(ServerLevel level, BlockPos controller) {
        report(level, controller, Kind.GAVE_UP);
    }

    /** {@code pos} is any crafter of the group. */
    public static void groupIncomplete(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            report(serverLevel, CreateCompat.controllerPos(level, pos).orElse(pos), Kind.GROUP_INCOMPLETE);
        }
    }

    /** @return whether the warning went out now (false while the same warning is still recent) */
    static boolean report(ServerLevel level, BlockPos controller, Kind kind) {
        long now = level.getGameTime();
        Key key = new Key(level.dimension(), controller.immutable(), kind);
        Long last = LAST.get(key);
        if (last != null && now >= last && now - last < INTERVAL_TICKS) {
            return false;
        }
        LAST.put(key, now);
        List<RegionOwners.Owned> owners = RegionOwners.of(level, controller);
        String label = owners.isEmpty() ? controller.toShortString() : owners.get(0).label();
        BigCreateAutoCrafter.LOGGER.warn("Crafter group '{}' at {}: {}", label, controller.toShortString(), kind);
        for (RegionOwners.Owned owned : owners) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owned.profile().owner());
            if (player != null) {
                player.sendSystemMessage(Component.translatable(MSG + kind.key, owned.label()).withStyle(kind.colour));
            }
        }
        return true;
    }

    public static void clear() {
        LAST.clear();
    }
}
