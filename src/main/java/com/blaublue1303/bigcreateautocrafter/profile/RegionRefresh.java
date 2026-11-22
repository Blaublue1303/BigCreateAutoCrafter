package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.Optional;

import com.blaublue1303.bigcreateautocrafter.Config;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;

import net.minecraft.server.level.ServerLevel;

/**
 * Keeps a stored {@link CrafterRegion} equal to the real crafters. The stored shape is only a snapshot from the moment
 * of registration; if the group was rebuilt, extended or got a new controller, patterns built from the snapshot would
 * put items into the wrong crafters. Checking against the world heals this without registering again.
 */
public final class RegionRefresh {
    private RegionRefresh() {
    }

    public enum State {
        /** The stored region still matches the crafters. */
        UNCHANGED,
        /** The crafters changed; {@link Result#region()} is the corrected region. */
        UPDATED,
        /** Nothing can be said now (a chunk of the group is not loaded) or the group is gone/incomplete; the region is kept as it is. */
        UNAVAILABLE
    }

    public record Result(State state, CrafterRegion region) {
    }

    public static Result check(ServerLevel level, CrafterRegion region) {
        // every 5 s per player: never load chunks for this, only look at groups that are fully loaded anyway
        if (!CreateCompat.isGroupLoaded(level, region.controller())) {
            return new Result(State.UNAVAILABLE, region);
        }
        Optional<ConnectedCrafterGroup> group = CreateCompat.findConnectedGroup(level, region.controller());
        if (group.isEmpty()) {
            return new Result(State.UNAVAILABLE, region);
        }
        var layout = RegionScanner.layoutOf(group.get(), Config.MAX_REGION_WIDTH.get(), Config.MAX_REGION_HEIGHT.get());
        if (layout.error().isPresent()) {
            return new Result(State.UNAVAILABLE, region);
        }
        CrafterRegion fresh = region.withScan(group.get().controller(), group.get().facing(), layout.getOrThrow(),
                CreateCompat.exitIndex(level, group.get()));
        return new Result(fresh.equals(region) ? State.UNCHANGED : State.UPDATED, fresh);
    }
}
