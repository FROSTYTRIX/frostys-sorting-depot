package net.frostytrix.sortingdepot.network;

import net.frostytrix.sortingdepot.client.TerminalClientHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Network payload registration. The only payload is the live {@link TerminalUpdatePayload} (server →
 * client); its handler lives in the client-only {@link TerminalClientHandler}, which is therefore never
 * classloaded on a dedicated server (play-to-client handlers are registered but only invoked client-side).
 */
public final class SDNetwork {

    private SDNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(
                TerminalUpdatePayload.TYPE,
                TerminalUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> TerminalClientHandler.handle(payload)));
    }
}
