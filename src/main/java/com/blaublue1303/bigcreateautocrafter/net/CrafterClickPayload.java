package com.blaublue1303.bigcreateautocrafter.net;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: profile-modifier + right-click on a Mechanical Crafter. {@code secondary} = Shift was held too. */
public record CrafterClickPayload(BlockPos pos, boolean secondary) implements CustomPacketPayload {
    public static final Type<CrafterClickPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BigCreateAutoCrafter.MODID, "crafter_click"));

    public static final StreamCodec<ByteBuf, CrafterClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CrafterClickPayload::pos,
            ByteBufCodecs.BOOL, CrafterClickPayload::secondary,
            CrafterClickPayload::new);

    @Override
    public Type<CrafterClickPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CrafterClickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ProfileService.handleCrafterClick(player, payload.pos(), payload.secondary());
            }
        });
    }
}
