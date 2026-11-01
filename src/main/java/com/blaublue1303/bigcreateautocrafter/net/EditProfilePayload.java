package com.blaublue1303.bigcreateautocrafter.net;

import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: an edit from the profile menu. The server checks that the player owns the profile. */
public record EditProfilePayload(UUID profileId, Action action, int regionIndex, String text) implements CustomPacketPayload {
    public enum Action {
        RENAME, DELETE, SET_ADDRESS, REMOVE_REGION
    }

    public static final Type<EditProfilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "edit_profile"));

    public static final StreamCodec<FriendlyByteBuf, EditProfilePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, EditProfilePayload::profileId,
            NeoForgeStreamCodecs.enumCodec(Action.class), EditProfilePayload::action,
            ByteBufCodecs.VAR_INT, EditProfilePayload::regionIndex,
            ByteBufCodecs.stringUtf8(256), EditProfilePayload::text,
            EditProfilePayload::new);

    @Override
    public Type<EditProfilePayload> type() {
        return TYPE;
    }

    static void handleOnServer(EditProfilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ProfileService.handleEdit(player, payload);
            }
        });
    }
}
