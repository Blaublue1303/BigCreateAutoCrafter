package com.blaublue1303.bigcreateautocrafter.net;

import java.util.List;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.client.ClientProfileCache;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: the full list of the receiving player's profiles (replaces the client cache). */
public record SyncProfilesPayload(List<CrafterProfile> profiles) implements CustomPacketPayload {
    public static final Type<SyncProfilesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "sync_profiles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncProfilesPayload> STREAM_CODEC =
            CrafterProfile.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(SyncProfilesPayload::new, SyncProfilesPayload::profiles);

    public SyncProfilesPayload {
        profiles = List.copyOf(profiles);
    }

    @Override
    public Type<SyncProfilesPayload> type() {
        return TYPE;
    }

    static void handleOnClient(SyncProfilesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientProfileCache.replace(payload.profiles()));
    }
}
