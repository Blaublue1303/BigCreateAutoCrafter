package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static final String MSG = "message." + BigCreateAutoCrafter.MODID + ".";

    /** Players currently holding the profile modifier key (reported by the client). */
    private static final Set<UUID> MODIFIER_HELD = ConcurrentHashMap.newKeySet();
    /** Profile each player edits with Ctrl + right-click. */
    private static final Map<UUID, UUID> ACTIVE_PROFILE = new ConcurrentHashMap<>();

    private ProfileService() {
    }

    public static void setModifierHeld(ServerPlayer player, boolean held) {
        if (held) {
            MODIFIER_HELD.add(player.getUUID());
        } else {
            MODIFIER_HELD.remove(player.getUUID());
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MODIFIER_HELD.remove(event.getEntity().getUUID());
        ACTIVE_PROFILE.remove(event.getEntity().getUUID());
    }

    /** Server side: suppress Create's normal crafter interaction while the player edits profiles. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !MODIFIER_HELD.contains(player.getUUID())) {
            return;
        }
        if (CreateCompat.isMechanicalCrafter(player.level().getBlockState(event.getPos()))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void handleCrafterClick(ServerPlayer player, BlockPos pos, boolean secondary) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        Optional<ConnectedCrafterGroup> group = CreateCompat.findConnectedGroup(level, pos);
        if (group.isEmpty()) {
            tell(player, Component.translatable(MSG + "not_a_group").withStyle(ChatFormatting.RED));
            return;
        }
        BlockPos controller = group.get().controller();
        ProfileManager manager = ProfileManager.get(player.server);
        List<CrafterProfile> own = manager.byOwner(player.getUUID());

        for (CrafterProfile profile : own) {
            int index = profile.dimension().equals(level.dimension()) ? profile.indexOfRegion(controller) : -1;
            if (index < 0) {
                continue;
            }
            CrafterRegion region = profile.regions().get(index);
            if (secondary) {
                removeRegion(player, manager, profile, index);
            } else {
                ACTIVE_PROFILE.put(player.getUUID(), profile.id());
                tell(player, Component.translatable(MSG + "region_exists", region.name(), profile.name()).withStyle(ChatFormatting.YELLOW));
            }
            return;
        }

        DataResult<List<String>> layout = RegionScanner.layoutOf(group.get(), Config.MAX_REGION_WIDTH.get(), Config.MAX_REGION_HEIGHT.get());
        if (layout.error().isPresent()) {
            tell(player, Component.translatable(MSG + "region_invalid", layout.error().get().message()).withStyle(ChatFormatting.RED));
            return;
        }
        CrafterRegion region = new CrafterRegion(CrafterRegion.sizeLabel(layout.getOrThrow()), controller, group.get().facing(),
                layout.getOrThrow(), "");

        Optional<CrafterProfile> active = secondary ? Optional.empty() : activeProfile(player, manager);
        if (active.isPresent()) {
            CrafterProfile profile = active.get();
            if (profile.regions().size() >= Config.MAX_REGIONS_PER_PROFILE.get()) {
                tell(player, Component.translatable(MSG + "limit_regions", Config.MAX_REGIONS_PER_PROFILE.get()).withStyle(ChatFormatting.RED));
                return;
            }
            manager.put(profile.withRegionAdded(region));
            tell(player, Component.translatable(MSG + "region_added", region.name(), profile.name()).withStyle(ChatFormatting.GREEN));
        } else {
            if (own.size() >= Config.MAX_PROFILES_PER_PLAYER.get()) {
                tell(player, Component.translatable(MSG + "limit_profiles", Config.MAX_PROFILES_PER_PLAYER.get()).withStyle(ChatFormatting.RED));
                return;
            }
            CrafterProfile profile = new CrafterProfile(UUID.randomUUID(), nextProfileName(own), level.dimension(), player.getUUID(), List.of(region));
            manager.put(profile);
            ACTIVE_PROFILE.put(player.getUUID(), profile.id());
            tell(player, Component.translatable(MSG + "profile_created", profile.name(), region.name()).withStyle(ChatFormatting.GREEN));
        }
        tell(player, Component.translatable(MSG + "address_hint").withStyle(ChatFormatting.GRAY));
        ProfileSync.syncTo(player);
    }

    private static void removeRegion(ServerPlayer player, ProfileManager manager, CrafterProfile profile, int index) {
        CrafterRegion region = profile.regions().get(index);
        if (profile.regions().size() == 1) {
            manager.remove(profile.id());
            ACTIVE_PROFILE.remove(player.getUUID(), profile.id());
            tell(player, Component.translatable(MSG + "profile_removed", profile.name()).withStyle(ChatFormatting.YELLOW));
        } else {
            manager.put(profile.withRegionRemoved(index));
            tell(player, Component.translatable(MSG + "region_removed", region.name(), profile.name()).withStyle(ChatFormatting.YELLOW));
        }
        ProfileSync.syncTo(player);
    }

    private static Optional<CrafterProfile> activeProfile(ServerPlayer player, ProfileManager manager) {
        UUID id = ACTIVE_PROFILE.get(player.getUUID());
        return Optional.ofNullable(id).flatMap(manager::byId)
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

    // ---- command helpers (indices are 1-based, as shown by /bcac list) ----

    public static Optional<CrafterProfile> profileByIndex(ServerPlayer player, int index) {
        List<CrafterProfile> own = ProfileManager.get(player.server).byOwner(player.getUUID());
        return index >= 1 && index <= own.size() ? Optional.of(own.get(index - 1)) : Optional.empty();
    }

    public static void select(ServerPlayer player, CrafterProfile profile) {
        ACTIVE_PROFILE.put(player.getUUID(), profile.id());
    }

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
