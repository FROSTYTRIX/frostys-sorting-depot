package net.frostytrix.sortingdepot.network;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the player typed a new name into the Linker Node GUI's name field. The server
 * validates that they can still reach the node and updates the BE. Edits are debounced on the
 * client to avoid flooding the server with one packet per keystroke.
 */
public record RenameLinkerNodePayload(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<RenameLinkerNodePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "rename_linker_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameLinkerNodePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RenameLinkerNodePayload::pos,
                    ByteBufCodecs.stringUtf8(LinkerNodeBlockEntity.MAX_NAME_LENGTH + 4), RenameLinkerNodePayload::name,
                    RenameLinkerNodePayload::new);

    @Override
    public Type<RenameLinkerNodePayload> type() {
        return TYPE;
    }

    public static void handle(RenameLinkerNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.level().getBlockEntity(payload.pos()) instanceof LinkerNodeBlockEntity node)) {
                return;
            }
            // Player must be close enough to be interacting with the node's menu.
            if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 64.0) {
                return;
            }
            node.setCustomName(payload.name());
        });
    }
}
