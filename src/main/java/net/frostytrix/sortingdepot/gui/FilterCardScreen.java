package net.frostytrix.sortingdepot.gui;

import java.util.List;

import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Configuration screen for a held Filter Card. Self-drawn (no background texture required): a panel,
 * three mode buttons, a row of five ghost slots showing the chosen items, and — in Tag mode — a scrolling
 * checklist of the tags those items belong to. Clicking an inventory item adds it; clicking a ghost slot
 * removes it; clicking a tag toggles it. All edits go to the server via {@link FilterCardMenu}'s buttons.
 */
public class FilterCardScreen extends AbstractContainerScreen<FilterCardMenu> {

    // Layout (relative to leftPos/topPos).
    private static final int MODE_Y = 20;
    private static final int MODE_H = 16;
    private static final int[] MODE_X = {8, 60, 112};
    private static final int MODE_W = 50;

    private static final int GHOST_Y = 42;
    private static final int GHOST_X = 8;
    private static final int SLOT = 18;

    private static final int TAGS_COUNT_Y = 63; // "Tags x/3" line, between ghost slots and the list
    private static final int TAG_Y = 76;
    private static final int TAG_ROW_H = 12;
    private static final int TAG_VISIBLE = 4;
    private static final int TAG_TEXT_X = 24; // where a tag row's name starts (after its checkbox)

    // Player-inventory slot frames. These are FRAME (top-left) coords; the 16px item renders 1px inside,
    // so the matching FilterCardMenu slots use these + 1. Keep the two in sync.
    private static final int INV_FRAME_X = 8;
    private static final int INV_FRAME_ROW0_Y = 139;
    private static final int HOTBAR_FRAME_Y = 197;
    private static final int GUI_HEIGHT = 222;

    // Colours (ARGB).
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF55555F;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_INNER = 0xFF373737;
    private static final int BTN = 0xFF6E6E6E;
    private static final int BTN_ACTIVE = 0xFF4E7A4E;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int CHECK_ON = 0xFF4E9E4E;

    private int tagScroll;

    public FilterCardScreen(FilterCardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = INV_FRAME_ROW0_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Panel.
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, PANEL_BORDER);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);

        // Player-inventory + hotbar slot frames (the base class renders the items into them, 1px inside).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + INV_FRAME_X + col * SLOT, y + INV_FRAME_ROW0_Y + row * SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + INV_FRAME_X + col * SLOT, y + HOTBAR_FRAME_Y);
        }

        FilterCardData d = menu.filterData();
        FilterCardData.Mode mode = d.mode();

        // Mode buttons.
        drawButton(graphics, x + MODE_X[0], y + MODE_Y, modeLabel("item"), mode == FilterCardData.Mode.ITEM);
        drawButton(graphics, x + MODE_X[1], y + MODE_Y, modeLabel("tag"), mode == FilterCardData.Mode.TAG);
        drawButton(graphics, x + MODE_X[2], y + MODE_Y, modeLabel("overflow"), mode == FilterCardData.Mode.OVERFLOW);

        if (mode == FilterCardData.Mode.OVERFLOW) {
            graphics.drawCenteredString(font, Component.translatable("gui.frostyssortingdepot.filter_card.overflow"),
                    x + imageWidth / 2, y + GHOST_Y + 8, TEXT);
        } else {
            // Ghost slots showing the chosen items.
            List<ResourceLocation> items = d.items();
            for (int i = 0; i < FilterCardData.MAX_ITEMS; i++) {
                int gx = x + GHOST_X + i * SLOT;
                drawSlot(graphics, gx, y + GHOST_Y);
                if (i < items.size()) {
                    ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(items.get(i)));
                    graphics.renderItem(icon, gx + 1, y + GHOST_Y + 1);
                }
            }

            if (mode == FilterCardData.Mode.ITEM) {
                graphics.drawString(font, Component.translatable("gui.frostyssortingdepot.filter_card.add_items",
                        items.size(), FilterCardData.MAX_ITEMS), x + 8, y + GHOST_Y + SLOT + 4, TEXT, false);
            } else {
                drawTagList(graphics, x, y, d, mouseX, mouseY);
            }
        }
    }

    private void drawTagList(GuiGraphics graphics, int x, int y, FilterCardData d, int mouseX, int mouseY) {
        List<ResourceLocation> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
        if (tags.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.frostyssortingdepot.filter_card.no_tags"),
                    x + 8, y + TAG_Y, TEXT, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.frostyssortingdepot.filter_card.tags",
                d.tags().size(), FilterCardData.MAX_TAGS), x + 8, y + TAGS_COUNT_Y, TEXT, false);
        if (tags.size() > TAG_VISIBLE) {
            graphics.drawString(font, Component.literal("▲▼"), x + imageWidth - 22, y + TAGS_COUNT_Y, TEXT, false);
        }

        int maxTextWidth = imageWidth - TAG_TEXT_X - 6;
        tagScroll = Math.clamp(tagScroll, 0, Math.max(0, tags.size() - TAG_VISIBLE));
        for (int r = 0; r < TAG_VISIBLE; r++) {
            int idx = tagScroll + r;
            if (idx >= tags.size()) {
                break;
            }
            ResourceLocation tag = tags.get(idx);
            int ry = y + TAG_Y + r * TAG_ROW_H;
            // Checkbox.
            graphics.fill(x + 10, ry, x + 20, ry + 10, SLOT_INNER);
            if (d.tags().contains(tag)) {
                graphics.fill(x + 12, ry + 2, x + 18, ry + 8, CHECK_ON);
            }
            // Tag name, ellipsized to fit; full id shown as a tooltip on hover.
            String full = "#" + tag;
            String shown = fitWidth(full, maxTextWidth);
            graphics.drawString(font, Component.literal(shown), x + TAG_TEXT_X, ry + 1, TEXT, false);
            if (!shown.equals(full) && inRect(mouseX - x, mouseY - y, 10, TAG_Y + r * TAG_ROW_H, imageWidth - 20, 10)) {
                graphics.renderTooltip(font, Component.literal(full), mouseX, mouseY);
            }
        }
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, Component label, boolean active) {
        graphics.fill(bx, by, bx + MODE_W, by + MODE_H, active ? BTN_ACTIVE : BTN);
        graphics.drawCenteredString(font, label, bx + MODE_W / 2, by + (MODE_H - 8) / 2, TEXT_WHITE);
    }

    private static void drawSlot(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + SLOT, sy + SLOT, SLOT_BG);
        graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, SLOT_INNER);
    }

    private static Component modeLabel(String mode) {
        return Component.translatable("item.frostyssortingdepot.filter_card.mode." + mode);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title + "Inventory" label (relative to the GUI origin).
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rx = (int) mouseX - leftPos;
            int ry = (int) mouseY - topPos;
            FilterCardData d = menu.filterData();
            FilterCardData.Mode mode = d.mode();

            // Mode buttons.
            if (inRect(rx, ry, MODE_X[0], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_ITEM);
            }
            if (inRect(rx, ry, MODE_X[1], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_TAG);
            }
            if (inRect(rx, ry, MODE_X[2], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_OVERFLOW);
            }

            if (mode != FilterCardData.Mode.OVERFLOW) {
                // Ghost-slot removal.
                for (int i = 0; i < FilterCardData.MAX_ITEMS; i++) {
                    if (inRect(rx, ry, GHOST_X + i * SLOT, GHOST_Y, SLOT, SLOT)) {
                        if (i < d.items().size()) {
                            return click(FilterCardMenu.BTN_REMOVE_ITEM + i);
                        }
                        return true; // empty ghost — consume, no-op
                    }
                }
                // Tag toggle.
                if (mode == FilterCardData.Mode.TAG) {
                    List<ResourceLocation> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
                    for (int r = 0; r < TAG_VISIBLE; r++) {
                        int idx = tagScroll + r;
                        if (idx < tags.size() && inRect(rx, ry, 10, TAG_Y + r * TAG_ROW_H, imageWidth - 20, 10)) {
                            return click(FilterCardMenu.BTN_TOGGLE_TAG + idx);
                        }
                    }
                }
                // Add an inventory item by clicking it.
                Slot hovered = this.hoveredSlot;
                if (hovered != null && hovered.hasItem() && !(hovered.getItem().getItem() instanceof FilterCardItem)) {
                    return click(FilterCardMenu.BTN_ADD_SLOT + hovered.index);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.filterData().mode() == FilterCardData.Mode.TAG) {
            FilterCardData d = menu.filterData();
            List<ResourceLocation> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
            int max = Math.max(0, tags.size() - TAG_VISIBLE);
            tagScroll = Math.clamp((int) (tagScroll - scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean click(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private static boolean inRect(int px, int py, int rx, int ry, int w, int h) {
        return px >= rx && px < rx + w && py >= ry && py < ry + h;
    }

    /** Truncates {@code s} with an ellipsis so it fits within {@code maxWidth} pixels. */
    private String fitWidth(String s, int maxWidth) {
        if (font.width(s) <= maxWidth) {
            return s;
        }
        return font.plainSubstrByWidth(s, maxWidth - font.width("…")) + "…";
    }
}
