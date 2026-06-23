package net.frostytrix.sortingdepot.item;

import java.util.function.Consumer;

import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A Filter Card. Stores a {@link FilterCardData} component describing what it accepts. Sneak-use
 * cycles the matching mode; the tooltip reflects the current configuration.
 *
 * <p>Setting a specific target item (Item mode) is done elsewhere (Linker GUI / a later phase); this
 * item only owns the held-hand mode cycling and tooltip for now.
 */
public class FilterCardItem extends Item {

    public FilterCardItem(Properties properties) {
        super(properties);
    }

    /** Reads the card's config, defaulting to {@link FilterCardData#EMPTY} when absent. */
    public static FilterCardData data(ItemStack stack) {
        return stack.getOrDefault(SDDataComponents.FILTER_DATA.get(), FilterCardData.EMPTY);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                FilterCardData current = data(stack);
                stack.set(SDDataComponents.FILTER_DATA.get(), current.withMode(current.nextMode()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Right-click this card (on the cursor) onto an item stack in a GUI to bind it to that item
     * (switches the card to Item mode). Returns {@code true} to consume the click instead of swapping.
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot other, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack target = other.getItem();
        if (target.isEmpty() || target.getItem() instanceof FilterCardItem) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(target.getItem());
        stack.set(SDDataComponents.FILTER_DATA.get(), FilterCardData.ofItem(id, false));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        FilterCardData d = data(stack);
        builder.accept(Component.translatable("item.frostyssortingdepot.filter_card.mode",
                        Component.translatable("item.frostyssortingdepot.filter_card.mode." + d.mode().getSerializedName()))
                .withStyle(ChatFormatting.GRAY));

        Component detail = switch (d.mode()) {
            case ITEM -> Component.translatable("item.frostyssortingdepot.filter_card.target",
                    d.itemId().map(Identifier::toString).orElse("—"));
            case TAG -> Component.translatable("item.frostyssortingdepot.filter_card.tags",
                    d.tags().isEmpty() ? "—" : String.valueOf(d.tags().size()));
            case OVERFLOW -> Component.translatable("item.frostyssortingdepot.filter_card.overflow");
        };
        builder.accept(detail.copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}
