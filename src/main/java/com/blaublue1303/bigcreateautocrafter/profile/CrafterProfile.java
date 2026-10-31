package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A crafter setup ("Anlage") created via Ctrl + right-click on a Mechanical Crafter. Holds one or more
 * {@link CrafterRegion}s, e.g. a large 21-crafter area plus a small 3×3 area.
 *
 * @param id        stable id
 * @param name      display name
 * @param dimension dimension the crafters are in
 * @param owner     player who created the profile
 * @param regions   craftable areas, in the order the player added them
 */
public record CrafterProfile(UUID id, String name, ResourceKey<Level> dimension, UUID owner, List<CrafterRegion> regions) {
    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<CrafterProfile> CODEC = RecordCodecBuilder.<CrafterProfile>create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CrafterProfile::id),
            Codec.STRING.fieldOf("name").forGetter(CrafterProfile::name),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(CrafterProfile::dimension),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(CrafterProfile::owner),
            CrafterRegion.CODEC.listOf().fieldOf("regions").forGetter(CrafterProfile::regions)
    ).apply(i, CrafterProfile::new)).validate(CrafterProfile::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterProfile> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CrafterProfile::id,
            ByteBufCodecs.STRING_UTF8, CrafterProfile::name,
            ResourceKey.streamCodec(Registries.DIMENSION), CrafterProfile::dimension,
            UUIDUtil.STREAM_CODEC, CrafterProfile::owner,
            CrafterRegion.STREAM_CODEC.apply(ByteBufCodecs.list()), CrafterProfile::regions,
            CrafterProfile::new);

    public CrafterProfile {
        regions = List.copyOf(regions);
    }

    public CrafterProfile withName(String newName) {
        return new CrafterProfile(id, newName, dimension, owner, regions);
    }

    public CrafterProfile withRegions(List<CrafterRegion> newRegions) {
        return new CrafterProfile(id, name, dimension, owner, newRegions);
    }

    public CrafterProfile withRegionAdded(CrafterRegion region) {
        List<CrafterRegion> list = new ArrayList<>(regions);
        list.add(region);
        return withRegions(list);
    }

    public CrafterProfile withRegionReplaced(int index, CrafterRegion region) {
        List<CrafterRegion> list = new ArrayList<>(regions);
        list.set(index, region);
        return withRegions(list);
    }

    public CrafterProfile withRegionRemoved(int index) {
        List<CrafterRegion> list = new ArrayList<>(regions);
        list.remove(index);
        return withRegions(list);
    }

    /** @return index of the region whose controller is {@code controller}, or -1 */
    public int indexOfRegion(BlockPos controller) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).controller().equals(controller)) {
                return i;
            }
        }
        return -1;
    }

    public static DataResult<CrafterProfile> validate(CrafterProfile profile) {
        if (profile.name().isBlank() || profile.name().length() > MAX_NAME_LENGTH) {
            return DataResult.error(() -> "Profile name must be 1-" + MAX_NAME_LENGTH + " characters: '" + profile.name() + "'");
        }
        if (profile.regions().isEmpty()) {
            return DataResult.error(() -> "Profile '" + profile.name() + "' has no regions");
        }
        return DataResult.success(profile);
    }
}
