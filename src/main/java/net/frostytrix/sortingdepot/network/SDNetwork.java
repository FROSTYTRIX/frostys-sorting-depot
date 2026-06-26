package net.frostytrix.sortingdepot.network;

import net.frostytrix.sortingdepot.client.TerminalClientHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Network payload registration:
 * <ul>
 *   <li>{@link TerminalUpdatePayload} (server → client) — live Depot Terminal refreshes.</li>
 *   <li>{@link AddFilterItemPayload} (client → server) — JEI/recipe-viewer ghost-slot drag.</li>
 * </ul>
 * Client handlers live in client-only classes so a dedicated server never classloads them.
 */
public final class SDNetwork {

    private SDNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(
                TerminalUpdatePayload.TYPE,
                TerminalUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> TerminalClientHandler.handle(payload)));
        registrar.playToServer(
                AddFilterItemPayload.TYPE,
                AddFilterItemPayload.STREAM_CODEC,
                AddFilterItemPayload::handle);
    }
}
