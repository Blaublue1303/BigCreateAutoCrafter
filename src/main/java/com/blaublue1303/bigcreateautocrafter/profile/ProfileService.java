package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.Config;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.mojang.serialization.DataResult;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Server-side profile editing: Ctrl + right-click on a crafter group, and the {@code /bcac} commands.
 *
 * <p>Ctrl + right-click: if the group is new, it is added as a region to the player's active profile (the one created
 * or edited last, same dimension); without an active profile a new profile is created. Ctrl + Shift + right-click
 * removes the group from its profile, or starts a new profile if the group is not registered yet.
 */
public final class ProfileService {
    static final String MSG = "message." + BigCreateAutoCrafter.MODID + ".";
    /** How long a profile click cancels the vanilla use of the clicked block (the use packet follows right after). */
    private static final int SUPPRESS_USE_TICKS = 20;

    private record SuppressedUse(BlockPos pos, long untilGameTime) {
    }

    /** Pending cancellation of Create's normal crafter interaction for the block a player just profile-clicked. */
    private static final Map<UUID, SuppressedUse> SUPPRESSED_USE = new ConcurrentHashMap<>();
    /** Profile each player edits with Ctrl + right-click. */
    private static final Map<UUID, UUID> ACTIVE_PROFILE = new ConcurrentHashMap<>();

    private ProfileService() {
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SUPPRESSED_USE.remove(event.getEntity().getUUID());
        ACTIVE_PROFILE.remove(event.getEntity().getUUID());
    }

    /**
     * Server side: the client sends the profile click before the vanilla use packet for the same click, so that use
     * (Create inserting/taking items) is cancelled here.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        SuppressedUse suppressed = player.level().isClientSide() ? null : SUPPRESSED_USE.get(player.getUUID());
        if (suppressed == null) {
            return;
        }
        boolean current = player.level().getGameTime() <= suppressed.untilGameTime();
        if (current && suppressed.pos().equals(event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
        if (!current || suppressed.pos().equals(event.getPos())) {
            SUPPRESSED_USE.remove(player.getUUID());
        }
    }

    public static void handleCrafterClick(ServerPlayer player, BlockPos pos, boolean secondary) {
        ServerLevel level = player.serverLevel();
        SUPPRESSED_USE.put(player.getUUID(), new SuppressedUse(pos.immutable(), level.getGameTime() + SUPPRESS_USE_TICKS));
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        Optional<ConnectedCrafterGroup> group = CreateCompat.findConnectedGroup(level, pos);
        if (group.isEmpty()) {
            tell(player, Component.translatable(MSG + "not_a_group").withStyle(ChatFormatting.RED));
            return;
        }
        BlockPos controller = group.get().controller();
        List<CrafterProfile> own = ProfileManager.get(player.server).byOwner(player.getUUID());

        for (CrafterProfile profile : own) {
            int index = profile.dimension().equals(level.dimension()) ? profile.indexOfRegion(controller) : -1;
            if (index < 0) {
                continue;
            }
            if (secondary) {
                removeRegion(player, profile, index);
            } else {
                ACTIVE_PROFILE.put(player.getUUID(), profile.id());
                tell(player, Component.translatable(MSG + "region_exists", profile.regions().get(index).name(), profile.name())
                        .withStyle(ChatFormatting.YELLOW));
            }
            return;
        }

        DataResult<List<String>> scanned = RegionScanner.layoutOf(group.get(), Config.MAX_REGION_WIDTH.get(), Config.MAX_REGION_HEIGHT.get());
        if (scanned.error().isPresent()) {
            tell(player, Component.translatable(MSG + "region_invalid", scanned.error().get().message()).withStyle(ChatFormatting.RED));
            return;
        }
        List<String> layout = scanned.getOrThrow();
        CrafterRegion region = new CrafterRegion(CrafterRegion.sizeLabel(layout), controller, group.get().facing(), layout, "");

        Optional<CrafterProfile> active = secondary ? Optional.empty() : activeProfile(player);
        if (active.isPresent()) {
            CrafterProfile profile = active.get();
            if (profile.regions().size() >= Config.MAX_REGIONS_PER_PROFILE.get()) {
                tell(player, Component.translatable(MSG + "limit_regions", Config.MAX_REGIONS_PER_PROFILE.get()).withStyle(ChatFormatting.RED));
                return;
            }
            save(player, profile.withRegionAdded(region));
            tell(player, Component.translatable(MSG + "region_added", region.name(), profile.name()).withStyle(ChatFormatting.GREEN));
        } else {
            if (own.size() >= Config.MAX_PROFILES_PER_PLAYER.get()) {
                tell(player, Component.translatable(MSG + "limit_profiles", Config.MAX_PROFILES_PER_PLAYER.get()).withStyle(ChatFormatting.RED));
                return;
            }
            CrafterProfile profile = new CrafterProfile(UUID.randomUUID(), nextProfileName(own), level.dimension(), player.getUUID(), List.of(region));
            save(player, profile);
            select(player, profile);
            tell(player, Component.translatable(MSG + "profile_created", profile.name(), region.name()).withStyle(ChatFormatting.GREEN));
        }
        tell(player, Component.translatable(MSG + "address_hint").withStyle(ChatFormatting.GRAY));
    }

    private static void removeRegion(ServerPlayer player, CrafterProfile profile, int index) {
        if (profile.regions().size() == 1) {
            delete(player, profile);
            tell(player, Component.translatable(MSG + "profile_removed", profile.name()).withStyle(ChatFormatting.YELLOW));
        } else {
            save(player, profile.withRegionRemoved(index));
            tell(player, Component.translatable(MSG + "region_removed", profile.regions().get(index).name(), profile.name())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static Optional<CrafterProfile> activeProfile(ServerPlayer player) {
        UUID id = ACTIVE_PROFILE.get(player.getUUID());
        return Optional.ofNullable(id).flatMap(ProfileManager.get(player.server)::byId)
                .filter(p -> p.owner().equals(player.getUUID()) && p.dimension().equals(player.level().dimension()));
    }

    private static String nextProfileName(List<CrafterProfile> own) {
        for (int n = 1; ; n++) {
            String candidate = "Anlage " + n;
            if (own.stream().noneMatch(p -> p.name().equals(candidate))) {
                return candidate;
            }
        }
    }

    // ---- shared with the /bcac commands (indices are 1-based, as shown by /bcac list) ----

    public static Optional<CrafterProfile> profileByIndex(ServerPlayer player, int index) {
        List<CrafterProfile> own = ProfileManager.get(player.server).byOwner(player.getUUID());
        return index >= 1 && index <= own.size() ? Optional.of(own.get(index - 1)) : Optional.empty();
    }

    public static void select(ServerPlayer player, CrafterProfile profile) {
        ACTIVE_PROFILE.put(player.getUUID(), profile.id());
    }

    /** Stores the profile (new or changed) and syncs the owner's client. */
    public static void save(ServerPlayer player, CrafterProfile profile) {
        ProfileManager.get(player.server).put(profile);
        ProfileSync.syncTo(player);
    }

    public static void delete(ServerPlayer player, CrafterProfile profile) {
        ProfileManager.get(player.server).remove(profile.id());
        ACTIVE_PROFILE.remove(player.getUUID(), profile.id());
        ProfileSync.syncTo(player);
    }

    private static void tell(ServerPlayer player, Component message) {
        player.displayClientMessage(message, false);
    }
}
