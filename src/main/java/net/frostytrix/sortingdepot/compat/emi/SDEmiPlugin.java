package net.frostytrix.sortingdepot.compat.emi;

import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.frostytrix.sortingdepot.gui.FilterCardScreen;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.network.AddFilterItemPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.item.ItemStack;

/**
 * EMI integration — the counterpart to {@code SDJeiPlugin}. Registers an {@link EmiDragDropHandler} for
 * the {@link FilterCardScreen} so an item dragged from EMI onto one of the five ghost slots is sent
 * server-side via {@link AddFilterItemPayload}. The whole plugin only classloads when EMI is present,
 * so a non-EMI install is unaffected.
 *
 * <p>EMI only publishes builds up to the 1.21.1 line, so this plugin lives on the {@code mc/1.21.1}
 * branch only; the other MC branches ship the JEI integration alone.
 */
@EmiEntrypoint
public class SDEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(FilterCardScreen.class, new FilterCardDropHandler());
    }

    /** Drops an EMI item onto whichever of the five ghost slots the cursor is over. */
    private static class FilterCardDropHandler implements EmiDragDropHandler<FilterCardScreen> {

        @Override
        public boolean dropStack(FilterCardScreen screen, EmiIngredient ingredient, int x, int y) {
            ItemStack stack = firstItem(ingredient);
            if (stack.isEmpty()) {
                return false;
            }
            // Overflow cards consume no items, so there are no ghost slots to accept into.
            if (screen.getMenu().filterData().mode() == FilterCardData.Mode.OVERFLOW) {
                return false;
            }
            int left = screen.getGuiLeft();
            int top = screen.getGuiTop();
            for (int i = 0; i < FilterCardData.MAX_ITEMS; i++) {
                int sx = left + FilterCardScreen.GHOST_X + i * FilterCardScreen.SLOT;
                int sy = top + FilterCardScreen.GHOST_Y;
                if (x >= sx && x < sx + FilterCardScreen.SLOT && y >= sy && y < sy + FilterCardScreen.SLOT) {
                    var player = Minecraft.getInstance().player;
                    if (player == null) {
                        return false;
                    }
                    // NeoForge's PacketDistributor has no sendToServer in this MC line; push the payload
                    // as a custom-payload packet through the active client connection instead.
                    player.connection.getConnection().send(
                            new ServerboundCustomPayloadPacket(new AddFilterItemPayload(stack.copyWithCount(1))));
                    return true;
                }
            }
            return false;
        }

        /** The first item-stack an EMI ingredient resolves to, or {@link ItemStack#EMPTY} for non-items. */
        private static ItemStack firstItem(EmiIngredient ingredient) {
            List<EmiStack> stacks = ingredient.getEmiStacks();
            return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0).getItemStack();
        }
    }
}
