package com.blaublue1303.bigcreateautocrafter.net;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: whether the profile-modifier key is held. Lets the server cancel Create's normal crafter
 * interaction for the click that edits a profile (the vanilla use-packet is still sent by the client).
 */
public record ModifierStatePayload(boolean held) implements CustomPacketPayload {
    public static final Type<ModifierStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "modifier_state"));

    public static final StreamCodec<ByteBuf, ModifierStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ModifierStatePayload::new, ModifierStatePayload::held);

    @Override
    public Type<ModifierStatePayload> type() {
        return TYPE;
    }

    static void handleOnServer(ModifierStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ProfileService.setModifierHeld(player, payload.held());
            }
        });
    }
}
