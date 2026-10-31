package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A crafter setup ("Anlage") the player registered, e.g. via Ctrl + right-click on a Mechanical Crafter.
 * A profile holds one or more {@link CrafterRegion}s (e.g. a large 21-crafter area plus a small 3×3 area).
 *
 * @param id        stable id
 * @param name      display name
 * @param dimension dimension the crafters are in
 * @param address   Create package address of the packager feeding this setup (may be empty until configured)
 * @param owner     player who created the profile
 * @param regions   craftable areas, in the order the player added them
 */
public record CrafterProfile(UUID id, String name, ResourceKey<Level> dimension, String address, UUID owner,
                             List<CrafterRegion> regions) {
    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<CrafterProfile> CODEC = RecordCodecBuilder.<CrafterProfile>create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CrafterProfile::id),
            Codec.STRING.fieldOf("name").forGetter(CrafterProfile::name),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(CrafterProfile::dimension),
            Codec.STRING.optionalFieldOf("address", "").forGetter(CrafterProfile::address),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(CrafterProfile::owner),
            CrafterRegion.CODEC.listOf().fieldOf("regions").forGetter(CrafterProfile::regions)
    ).apply(i, CrafterProfile::new)).validate(CrafterProfile::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterProfile> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CrafterProfile::id,
            ByteBufCodecs.STRING_UTF8, CrafterProfile::name,
            ResourceKey.streamCodec(Registries.DIMENSION), CrafterProfile::dimension,
            ByteBufCodecs.STRING_UTF8, CrafterProfile::address,
            UUIDUtil.STREAM_CODEC, CrafterProfile::owner,
            CrafterRegion.STREAM_CODEC.apply(ByteBufCodecs.list()), CrafterProfile::regions,
            CrafterProfile::new);

    public CrafterProfile {
        regions = List.copyOf(regions);
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
