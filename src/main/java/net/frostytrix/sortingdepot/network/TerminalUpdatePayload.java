package net.frostytrix.sortingdepot.network;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.gui.TerminalSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: a fresh {@link TerminalSnapshot} for an open Depot Terminal, sent periodically while
 * the menu is open so the dashboard updates live instead of only at open time.
 */
public record TerminalUpdatePayload(TerminalSnapshot snapshot) implements CustomPacketPayload {

    public static final Type<TerminalUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "terminal_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalUpdatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.snapshot.write(buf),
                    buf -> new TerminalUpdatePayload(TerminalSnapshot.read(buf)));

    @Override
    public Type<TerminalUpdatePayload> type() {
        return TYPE;
    }
}
