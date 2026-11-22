package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.ArrayList;
import java.util.List;

import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Which players registered a crafter group as a region (server side, all profiles). */
public final class RegionOwners {
    private RegionOwners() {
    }

    /** A region of a profile. */
    public record Owned(CrafterProfile profile, int regionIndex) {
        public CrafterRegion region() {
            return profile.regions().get(regionIndex);
        }

        /** "Profile · Region", as shown to players. */
        public String label() {
            return profile.name() + " · " + region().name();
        }
    }

    /**
     * Regions (of any player, in this dimension) that are the crafter group with this controller. A region counts when
     * its stored controller is any member of the group, since the stored controller may be from before a rebuild.
     */
    public static List<Owned> of(ServerLevel level, BlockPos controller) {
        List<BlockPos> members = CreateCompat.findConnectedGroup(level, controller)
                .map(ConnectedCrafterGroup::members)
                .orElse(List.of(controller));
        List<Owned> owned = new ArrayList<>();
        for (CrafterProfile profile : ProfileManager.get(level.getServer()).all()) {
            if (!profile.dimension().equals(level.dimension())) {
                continue;
            }
            for (int i = 0; i < profile.regions().size(); i++) {
                if (members.contains(profile.regions().get(i).controller())) {
                    owned.add(new Owned(profile, i));
                }
            }
        }
        return owned;
    }
}
