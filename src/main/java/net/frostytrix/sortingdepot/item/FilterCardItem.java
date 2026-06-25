package net.frostytrix.sortingdepot.item;

import java.util.function.Consumer;

import net.frostytrix.sortingdepot.gui.FilterCardMenu;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A Filter Card. Stores a {@link FilterCardData} component describing what it accepts. The card is
 * configured entirely in the <em>Linker Node GUI</em> (place the card in a node and pick items/tags via
 * the ghost slots and tag checklist) — the item itself is just a carrier. The tooltip summarizes the
 * current configuration.
 */
public class FilterCardItem extends Item {

    public FilterCardItem(Properties properties) {
        super(properties);
    }

    /** Reads the card's config, defaulting to {@link FilterCardData#EMPTY} when absent. */
    public static FilterCardData data(ItemStack stack) {
        return stack.getOrDefault(SDDataComponents.FILTER_DATA.get(), FilterCardData.EMPTY);
    }

    /** Right-click (in air) opens the configuration GUI for the held card. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new FilterCardMenu(id, inv, hand), held.getHoverName()),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        FilterCardData d = data(stack);
        builder.accept(Component.translatable("item.frostyssortingdepot.filter_card.mode",
                        Component.translatable("item.frostyssortingdepot.filter_card.mode." + d.mode().getSerializedName()))
                .withStyle(ChatFormatting.GRAY));

        Component detail = switch (d.mode()) {
            case ITEM -> Component.translatable(d.strict()
                            ? "item.frostyssortingdepot.filter_card.items_strict"
                            : "item.frostyssortingdepot.filter_card.items",
                    d.items().size(), FilterCardData.MAX_ITEMS);
            case MOD -> Component.translatable("item.frostyssortingdepot.filter_card.mods", d.items().size());
            case TAG -> Component.translatable("item.frostyssortingdepot.filter_card.tags",
                    d.tags().size(), FilterCardData.MAX_TAGS);
            case OVERFLOW -> Component.translatable("item.frostyssortingdepot.filter_card.overflow");
        };
        builder.accept(detail.copy().withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("item.frostyssortingdepot.filter_card.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
