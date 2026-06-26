package net.frostytrix.sortingdepot.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.gui.FilterCardScreen;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.network.AddFilterItemPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI integration. Registers an {@link IGhostIngredientHandler} for the {@link FilterCardScreen} so an
 * item dragged from JEI onto one of the five ghost slots is sent server-side via
 * {@link AddFilterItemPayload}. The whole plugin only classloads when JEI is present, so a non-JEI
 * install is unaffected.
 */
@JeiPlugin
public class SDJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(FilterCardScreen.class, new FilterCardGhostHandler());
    }

    /** Maps the screen's five ghost-slot rects to a server-side "add this item" packet per slot. */
    private static class FilterCardGhostHandler implements IGhostIngredientHandler<FilterCardScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(FilterCardScreen screen,
                                                   ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            // Only react to item-stack drops, and only when the card is in a mode that consumes items.
            if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
                return List.of();
            }
            FilterCardData data = screen.getMenu().filterData();
            if (data.mode() == FilterCardData.Mode.OVERFLOW) {
                return List.of();
            }
            List<Target<I>> targets = new ArrayList<>(FilterCardData.MAX_ITEMS);
            int x = screen.getGuiLeft();
            int y = screen.getGuiTop();
            for (int i = 0; i < FilterCardData.MAX_ITEMS; i++) {
                Rect2i rect = new Rect2i(
                        x + FilterCardScreen.GHOST_X + i * FilterCardScreen.SLOT,
                        y + FilterCardScreen.GHOST_Y,
                        FilterCardScreen.SLOT,
                        FilterCardScreen.SLOT);
                targets.add(new GhostTarget<>(rect));
            }
            return targets;
        }

        @Override
        public boolean shouldHighlightTargets() {
            return true;
        }

        @Override
        public void onComplete() {
            // No client-side cleanup; the server applies the change and the card component sync handles
            // the UI refresh automatically.
        }
    }

    /** One ghost-slot rect. Accepting any item sends an AddFilterItemPayload to the server. */
    private record GhostTarget<I>(Rect2i area) implements IGhostIngredientHandler.Target<I> {

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            if (!(ingredient instanceof ItemStack stack) || stack.isEmpty()) {
                return;
            }
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            // NeoForge's PacketDistributor has no sendToServer in this MC line; push the payload as
            // a custom-payload packet through the active client connection instead.
            player.connection.getConnection().send(
                    new ServerboundCustomPayloadPacket(new AddFilterItemPayload(stack.copyWithCount(1))));
        }
    }
}
