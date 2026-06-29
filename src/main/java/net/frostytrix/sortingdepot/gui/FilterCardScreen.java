package net.frostytrix.sortingdepot.gui;

import java.util.List;
import java.util.stream.Collectors;

import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Configuration screen for a held Filter Card. Self-drawn (no background texture required): a panel, four
 * mode buttons (Item / Mod / Tag / Any), a row of five ghost slots showing the chosen items, and a
 * mode-specific area below — a strict-NBT toggle in Item mode, the matched namespaces in Mod mode, or a
 * scrolling tag checklist in Tag mode. Click an inventory item to add it; click a ghost slot to remove it.
 * All edits go to the server via {@link FilterCardMenu}'s buttons.
 */
public class FilterCardScreen extends AbstractContainerScreen<FilterCardMenu> {

    // Layout (relative to leftPos/topPos).
    private static final int MODE_Y = 20;
    private static final int MODE_H = 16;
    private static final int[] MODE_X = {8, 50, 92, 134};
    private static final int MODE_W = 39;

    // NOT/exclude toggle: a small button at the top-right of the panel, visible in every mode.
    private static final int NOT_X = 138;
    private static final int NOT_Y = 4;
    private static final int NOT_W = 30;
    private static final int NOT_H = 10;
    private static final int NOT_ACTIVE = 0xFFA85A5A;

    public static final int GHOST_Y = 42;
    public static final int GHOST_X = 8;
    public static final int SLOT = 18;

    private static final int DETAIL_Y = 64; // strict toggle (Item) / namespaces (Mod) / tag count (Tag)
    private static final int TAG_Y = 78;
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
        super(menu, inventory, title, 176, GUI_HEIGHT);
        this.inventoryLabelY = INV_FRAME_ROW0_Y - 12;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
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
        drawButton(graphics, x + MODE_X[0], y + MODE_Y, btnLabel("item"), mode == FilterCardData.Mode.ITEM);
        drawButton(graphics, x + MODE_X[1], y + MODE_Y, btnLabel("mod"), mode == FilterCardData.Mode.MOD);
        drawButton(graphics, x + MODE_X[2], y + MODE_Y, btnLabel("tag"), mode == FilterCardData.Mode.TAG);
        drawButton(graphics, x + MODE_X[3], y + MODE_Y, btnLabel("any"), mode == FilterCardData.Mode.OVERFLOW);

        // NOT/exclude toggle.
        boolean negated = d.negated();
        int notBg = negated ? NOT_ACTIVE : BTN;
        graphics.fill(x + NOT_X, y + NOT_Y, x + NOT_X + NOT_W, y + NOT_Y + NOT_H, notBg);
        graphics.centeredText(font, btnLabel("not"), x + NOT_X + NOT_W / 2, y + NOT_Y + 1, TEXT_WHITE);
        if (inRect(mouseX - x, mouseY - y, NOT_X, NOT_Y, NOT_W, NOT_H)) {
            graphics.setTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.frostyssortingdepot.filter_card.not.tooltip.title").getVisualOrderText(),
                    Component.translatable("gui.frostyssortingdepot.filter_card.not.tooltip.line1").getVisualOrderText(),
                    Component.translatable("gui.frostyssortingdepot.filter_card.not.tooltip.line2").getVisualOrderText(),
                    Component.translatable("gui.frostyssortingdepot.filter_card.not.tooltip.line3").getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, false);
        }

        if (mode == FilterCardData.Mode.OVERFLOW) {
            graphics.centeredText(font, Component.translatable("gui.frostyssortingdepot.filter_card.overflow"),
                    x + imageWidth / 2, y + GHOST_Y + 8, TEXT);
            return;
        }

        // Ghost slots showing the chosen reference items (shared by Item/Mod/Tag).
        List<ItemStack> items = d.items();
        for (int i = 0; i < FilterCardData.MAX_ITEMS; i++) {
            int gx = x + GHOST_X + i * SLOT;
            drawSlot(graphics, gx, y + GHOST_Y);
            if (i < items.size()) {
                graphics.item(items.get(i), gx + 1, y + GHOST_Y + 1);
            }
        }

        switch (mode) {
            case ITEM -> {
                // Strict-NBT checkbox.
                int cy = y + DETAIL_Y;
                graphics.fill(x + 10, cy, x + 20, cy + 10, SLOT_INNER);
                if (d.strict()) {
                    graphics.fill(x + 12, cy + 2, x + 18, cy + 8, CHECK_ON);
                }
                graphics.text(font, Component.translatable("gui.frostyssortingdepot.filter_card.strict"),
                        x + 24, cy + 1, TEXT, false);
                graphics.text(font, Component.translatable("gui.frostyssortingdepot.filter_card.add_items",
                        items.size(), FilterCardData.MAX_ITEMS), x + 8, y + DETAIL_Y + 14, TEXT, false);
            }
            case MOD -> {
                String mods = items.stream()
                        .map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())
                        .distinct().collect(Collectors.joining(", "));
                Component line = mods.isEmpty()
                        ? Component.translatable("gui.frostyssortingdepot.filter_card.no_mods")
                        : Component.translatable("gui.frostyssortingdepot.filter_card.mods",
                                fitWidth(mods, imageWidth - 40));
                graphics.text(font, line, x + 8, y + DETAIL_Y, TEXT, false);
            }
            case TAG -> drawTagList(graphics, x, y, d, mouseX, mouseY);
            default -> { }
        }
    }

    private void drawTagList(GuiGraphicsExtractor graphics, int x, int y, FilterCardData d, int mouseX, int mouseY) {
        List<Identifier> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
        if (tags.isEmpty()) {
            graphics.text(font, Component.translatable("gui.frostyssortingdepot.filter_card.no_tags"),
                    x + 8, y + DETAIL_Y, TEXT, false);
            return;
        }
        graphics.text(font, Component.translatable("gui.frostyssortingdepot.filter_card.tags",
                d.tags().size(), FilterCardData.MAX_TAGS), x + 8, y + DETAIL_Y, TEXT, false);
        if (tags.size() > TAG_VISIBLE) {
            graphics.text(font, Component.literal("▲▼"), x + imageWidth - 22, y + DETAIL_Y, TEXT, false);
        }

        int maxTextWidth = imageWidth - TAG_TEXT_X - 6;
        tagScroll = Math.clamp(tagScroll, 0, Math.max(0, tags.size() - TAG_VISIBLE));
        for (int r = 0; r < TAG_VISIBLE; r++) {
            int idx = tagScroll + r;
            if (idx >= tags.size()) {
                break;
            }
            Identifier tag = tags.get(idx);
            int ry = y + TAG_Y + r * TAG_ROW_H;
            // Checkbox.
            graphics.fill(x + 10, ry, x + 20, ry + 10, SLOT_INNER);
            if (d.tags().contains(tag)) {
                graphics.fill(x + 12, ry + 2, x + 18, ry + 8, CHECK_ON);
            }
            // Tag name, ellipsized to fit; full id shown as a tooltip on hover.
            String full = "#" + tag;
            String shown = fitWidth(full, maxTextWidth);
            graphics.text(font, Component.literal(shown), x + TAG_TEXT_X, ry + 1, TEXT, false);
            if (!shown.equals(full) && inRect(mouseX - x, mouseY - y, 10, TAG_Y + r * TAG_ROW_H, imageWidth - 20, 10)) {
                graphics.setTooltipForNextFrame(Component.literal(full), mouseX, mouseY);
            }
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int bx, int by, Component label, boolean active) {
        graphics.fill(bx, by, bx + MODE_W, by + MODE_H, active ? BTN_ACTIVE : BTN);
        graphics.centeredText(font, label, bx + MODE_W / 2, by + (MODE_H - 8) / 2, TEXT_WHITE);
    }

    private static void drawSlot(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + SLOT, sy + SLOT, SLOT_BG);
        graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, SLOT_INNER);
    }

    private static Component btnLabel(String key) {
        return Component.translatable("gui.frostyssortingdepot.filter_card.btn." + key);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title + "Inventory" label (default positions).
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int rx = (int) event.x() - leftPos;
            int ry = (int) event.y() - topPos;
            FilterCardData d = menu.filterData();
            FilterCardData.Mode mode = d.mode();

            // Mode buttons.
            if (inRect(rx, ry, MODE_X[0], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_ITEM);
            }
            if (inRect(rx, ry, MODE_X[1], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_MOD);
            }
            if (inRect(rx, ry, MODE_X[2], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_TAG);
            }
            if (inRect(rx, ry, MODE_X[3], MODE_Y, MODE_W, MODE_H)) {
                return click(FilterCardMenu.BTN_MODE_OVERFLOW);
            }

            // NOT/exclude toggle (top-right, visible in every mode).
            if (inRect(rx, ry, NOT_X, NOT_Y, NOT_W, NOT_H)) {
                return click(FilterCardMenu.BTN_TOGGLE_NEGATED);
            }

            if (mode != FilterCardData.Mode.OVERFLOW) {
                // Strict toggle (Item mode only).
                if (mode == FilterCardData.Mode.ITEM && inRect(rx, ry, 10, DETAIL_Y, 120, 10)) {
                    return click(FilterCardMenu.BTN_TOGGLE_STRICT);
                }
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
                    List<Identifier> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
                    for (int r = 0; r < TAG_VISIBLE; r++) {
                        int idx = tagScroll + r;
                        if (idx < tags.size() && inRect(rx, ry, 10, TAG_Y + r * TAG_ROW_H, imageWidth - 20, 10)) {
                            return click(FilterCardMenu.BTN_TOGGLE_TAG + idx);
                        }
                    }
                }
                // Add an inventory item by clicking it.
                Slot hovered = getHoveredSlot();
                if (hovered != null && hovered.hasItem() && !(hovered.getItem().getItem() instanceof FilterCardItem)) {
                    return click(FilterCardMenu.BTN_ADD_SLOT + hovered.index);
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.filterData().mode() == FilterCardData.Mode.TAG) {
            FilterCardData d = menu.filterData();
            List<Identifier> tags = FilterCardMenu.displayedTags(d.items(), d.tags());
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
