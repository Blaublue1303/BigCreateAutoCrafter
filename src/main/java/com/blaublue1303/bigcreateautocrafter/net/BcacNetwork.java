package com.blaublue1303.bigcreateautocrafter.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers all network payloads of the mod. Client → server payloads (create profile, add region,
 * order request) are added here in the later implementation steps (docs/06-todo.md, steps 5 and 8).
 */
public final class BcacNetwork {
    /** Bump on any incompatible payload change. */
    public static final String PROTOCOL_VERSION = "1";

    private BcacNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncProfilesPayload.TYPE, SyncProfilesPayload.STREAM_CODEC, SyncProfilesPayload::handleOnClient);
    }
}
