package com.blaublue1303.bigcreateautocrafter.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers all network payloads of the mod. */
public final class BcacNetwork {
    /** Bump on any incompatible payload change. */
    public static final String PROTOCOL_VERSION = "3";

    private BcacNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncProfilesPayload.TYPE, SyncProfilesPayload.STREAM_CODEC, SyncProfilesPayload::handleOnClient);
        registrar.playToServer(CrafterClickPayload.TYPE, CrafterClickPayload.STREAM_CODEC, CrafterClickPayload::handleOnServer);
    }
}
