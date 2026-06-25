package net.frostytrix.sortingdepot.item;

import java.util.List;

import net.frostytrix.sortingdepot.gui.FilterCardMenu;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Filter Card. Stores a {@link FilterCardData} component describing what it accepts. The card is
 * configured in a dedicated GUI (right-click the held card) — the tooltip summarizes the current state.
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new FilterCardMenu(id, inv, hand), held.getHoverName()),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FilterCardData d = data(stack);
        tooltip.add(Component.translatable("item.frostyssortingdepot.filter_card.mode",
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
        tooltip.add(detail.copy().withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.frostyssortingdepot.filter_card.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
