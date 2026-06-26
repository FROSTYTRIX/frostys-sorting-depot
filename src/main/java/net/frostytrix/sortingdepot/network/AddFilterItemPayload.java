package net.frostytrix.sortingdepot.network;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.gui.FilterCardMenu;
import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the player dragged an item from JEI (or another recipe viewer) onto a Filter Card
 * ghost slot. The server validates that they have a {@link FilterCardMenu} open and adds the item to
 * the card's filter via the existing {@link FilterCardData} helper.
 */
public record AddFilterItemPayload(ItemStack stack) implements CustomPacketPayload {

    public static final Type<AddFilterItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "add_filter_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddFilterItemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, AddFilterItemPayload::stack,
                    AddFilterItemPayload::new);

    @Override
    public Type<AddFilterItemPayload> type() {
        return TYPE;
    }

    /** Server-side handler. Adds the dragged item to the held Filter Card via the menu's helper. */
    public static void handle(AddFilterItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof FilterCardMenu menu)) {
                return;
            }
            ItemStack card = menu.getCard();
            if (!(card.getItem() instanceof FilterCardItem)) {
                return;
            }
            ItemStack toAdd = payload.stack();
            if (toAdd.isEmpty() || toAdd.getItem() instanceof FilterCardItem) {
                return;
            }
            FilterCardData updated = FilterCardItem.data(card).withItemAdded(toAdd);
            card.set(SDDataComponents.FILTER_DATA.get(), updated);
        });
    }
}
