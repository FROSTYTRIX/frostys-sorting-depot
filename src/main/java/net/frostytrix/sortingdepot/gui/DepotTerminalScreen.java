package net.frostytrix.sortingdepot.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.client.SDLinkerBeams;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Terminal: a read-only network dashboard listing each Linker Node's target,
 * filter, and fill, plus the Overflow Chest fill and the Controller's buffer count. Rows that are too
 * wide wrap onto extra lines (keeping the row's left offset, with the fill % trailing at the end); the
 * list scrolls per-entry when there are more rows than fit. A search box at the top filters rows by
 * target name OR filter text (case-insensitive substring); clicking a row briefly highlights the
 * corresponding Linker Node in-world.
 */
public class DepotTerminalScreen extends AbstractContainerScreen<DepotTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_terminal.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ROW_HEIGHT = 10;
    private static final int SEARCH_WIDTH = 150;
    private static final int SEARCH_HEIGHT = 12;
    private static final int LIST_TOP = 18 + SEARCH_HEIGHT + 2;
    private static final int ESC_KEY = 256;
    private static final int LIST_BOTTOM_MARGIN = 36; // reserved for the overflow + buffer lines
    private static final long HIGHLIGHT_DURATION_MS = 5_000L;

    private int scrollOffset; // index of the first entry shown
    private @org.jetbrains.annotations.Nullable EditBox searchBox;

    /** Per-drawn-row hit boxes (in screen space), recorded each frame so {@link #mouseClicked} can map clicks back to entries. */
    private final List<RowHit> rowHits = new ArrayList<>();

    private record RowHit(int top, int bottom, TerminalSnapshot.Entry entry) {
    }

    public DepotTerminalScreen(DepotTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2 + 8;
        int y = (this.height - this.imageHeight) / 2 + 18;
        EditBox previous = this.searchBox;
        this.searchBox = new EditBox(this.font, x, y, SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.translatable("gui.frostyssortingdepot.terminal.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setBordered(true);
        this.searchBox.setHint(Component.translatable("gui.frostyssortingdepot.terminal.search.hint"));
        if (previous != null) {
            this.searchBox.setValue(previous.getValue());
        }
        addRenderableWidget(this.searchBox);
    }

    /** Bottom of the entry-drawing area; one row is held back for the scroll indicator. */
    private int entryAreaBottom() {
        return this.imageHeight - LIST_BOTTOM_MARGIN - ROW_HEIGHT;
    }

    private int rowWidth() {
        return this.imageWidth - 16; // 8px padding on each side
    }

    /** The wrapped display lines for one entry: {@code target · filter · fill%}, wrapped to the row width. */
    private List<FormattedCharSequence> entryLines(TerminalSnapshot.Entry entry) {
        String full = entry.target() + "  ·  " + entry.filter() + "  ·  " + entry.fill() + "%";
        return this.font.split(Component.literal(full), rowWidth());
    }

    /** Entries that pass the search filter (case-insensitive substring on target OR filter). */
    private List<TerminalSnapshot.Entry> visibleEntries() {
        List<TerminalSnapshot.Entry> all = this.menu.getSnapshot().linkers();
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return all;
        }
        List<TerminalSnapshot.Entry> out = new ArrayList<>(all.size());
        for (TerminalSnapshot.Entry e : all) {
            if (e.target().toLowerCase(Locale.ROOT).contains(query)
                    || e.filter().toLowerCase(Locale.ROOT).contains(query)) {
                out.add(e);
            }
        }
        return out;
    }

    /** The largest scroll offset that still fills the list from the bottom (so every entry stays reachable). */
    private int maxScroll() {
        List<TerminalSnapshot.Entry> linkers = visibleEntries();
        int capacity = entryAreaBottom() - LIST_TOP;
        int used = 0;
        for (int i = linkers.size() - 1; i >= 0; i--) {
            used += entryLines(linkers.get(i)).size() * ROW_HEIGHT;
            if (used > capacity) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Row hit-tests first: AbstractContainerScreen.mouseClicked returns true for
        // empty-area clicks too (slot drag bookkeeping), which would swallow our row clicks.
        if (button == 0) {
            int guiX = (this.width - this.imageWidth) / 2;
            for (RowHit hit : rowHits) {
                if (mouseY >= hit.top && mouseY < hit.bottom
                        && mouseX >= guiX + 8 && mouseX < guiX + 8 + rowWidth()) {
                    SDLinkerBeams.highlight(hit.entry.pos(), HIGHLIGHT_DURATION_MS);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep ESC working to close. For every other key, when the search box has focus,
        // route into it and swallow the key so the inventory-close keybind ("E" by default)
        // doesn't fire while the user is typing.
        if (this.searchBox != null && this.searchBox.isFocused() && keyCode != ESC_KEY) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOR, false);

        rowHits.clear();
        int guiY = (this.height - this.imageHeight) / 2;
        List<TerminalSnapshot.Entry> linkers = visibleEntries();
        if (linkers.isEmpty()) {
            String key = this.menu.getSnapshot().linkers().isEmpty()
                    ? "gui.frostyssortingdepot.terminal.empty"
                    : "gui.frostyssortingdepot.terminal.no_match";
            graphics.drawString(this.font, Component.translatable(key), 8, LIST_TOP, TEXT_COLOR, false);
        } else {
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
            int bottom = entryAreaBottom();
            int y = LIST_TOP;
            int i = scrollOffset;
            for (; i < linkers.size(); i++) {
                TerminalSnapshot.Entry entry = linkers.get(i);
                List<FormattedCharSequence> lines = entryLines(entry);
                int rowHeightPx = lines.size() * ROW_HEIGHT;
                if (y + rowHeightPx > bottom) {
                    break; // next entry would overflow the list area
                }
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(this.font, line, 8, y, TEXT_COLOR, false);
                    y += ROW_HEIGHT;
                }
                rowHits.add(new RowHit(guiY + y - rowHeightPx, guiY + y, entry));
            }
            // Scroll affordances, both arrows together below the last drawn entry.
            boolean hasAbove = scrollOffset > 0;
            boolean hasBelow = i < linkers.size();
            if (hasAbove || hasBelow) {
                StringBuilder indicator = new StringBuilder();
                if (hasAbove) {
                    indicator.append("▲");
                }
                if (hasBelow) {
                    if (hasAbove) {
                        indicator.append(' ');
                    }
                    indicator.append("▼ ").append(linkers.size() - i).append(" more");
                }
                graphics.drawString(this.font, Component.literal(indicator.toString()), 8, y, TEXT_COLOR, false);
            }
        }

        Component overflow = Component.translatable("gui.frostyssortingdepot.terminal.overflow",
                this.menu.getSnapshot().hasOverflow() ? this.menu.getSnapshot().overflowFill() + "%" : "-");
        Component buffer = Component.translatable("gui.frostyssortingdepot.terminal.buffer",
                this.menu.getSnapshot().inputCount());
        graphics.drawString(this.font, overflow, 8, this.imageHeight - 32, TEXT_COLOR, false);
        graphics.drawString(this.font, buffer, 8, this.imageHeight - 22, TEXT_COLOR, false);
    }
}
