package com.blaublue1303.bigcreateautocrafter.profile;

import com.blaublue1303.bigcreateautocrafter.net.SyncProfilesPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends a player their own profiles (on login; later also after every change). */
public final class ProfileSync {
    private ProfileSync() {
    }

    public static void syncTo(ServerPlayer player) {
        var profiles = ProfileManager.get(player.server).byOwner(player.getUUID());
        PacketDistributor.sendToPlayer(player, new SyncProfilesPayload(profiles));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }
}
