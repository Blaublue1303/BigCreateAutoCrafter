package com.blaublue1303.bigcreateautocrafter.client;

import java.util.List;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;

/**
 * Client-side copy of the local player's profiles, filled by {@code SyncProfilesPayload}.
 * Contains no client-only Minecraft classes, so it is safe to reference from common network code.
 */
public final class ClientProfileCache {
    private static volatile List<CrafterProfile> profiles = List.of();

    private ClientProfileCache() {
    }

    public static List<CrafterProfile> all() {
        return profiles;
    }

    public static void replace(List<CrafterProfile> newProfiles) {
        profiles = List.copyOf(newProfiles);
    }

    public static void clear() {
        profiles = List.of();
    }
}
