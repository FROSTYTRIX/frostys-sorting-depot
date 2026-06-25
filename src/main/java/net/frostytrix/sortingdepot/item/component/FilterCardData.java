package net.frostytrix.sortingdepot.item.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.frostytrix.sortingdepot.routing.FilterMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * The data component stored on a Filter Card. This is the Minecraft-facing mirror of the pure
 * {@link FilterMode}: it holds real {@link ItemStack}s (so strict matching has components to compare and
 * Mod mode has a namespace source) plus tag ids, then {@linkplain #toFilterMode() converts} to the
 * Minecraft-free routing type at the adapter boundary.
 *
 * <p>A card holds up to {@value #MAX_ITEMS} items and up to {@value #MAX_TAGS} tags. The active
 * {@link Mode} decides what is consulted when routing:
 * <ul>
 *   <li>{@code ITEM} — the item ids (and, when {@link #strict}, their components too);</li>
 *   <li>{@code MOD} — the namespaces of the listed items (e.g. everything from {@code create});</li>
 *   <li>{@code TAG} — the chosen tags;</li>
 *   <li>{@code OVERFLOW} — everything.</li>
 * </ul>
 * The item list and tags are retained across mode switches so the GUI can rebuild its pickers.
 *
 * @param mode   which matching strategy this card uses
 * @param items  reference item stacks (Item/Mod modes)
 * @param tags   tag keys (Tag mode)
 * @param strict whether Item mode also compares item components
 */
public record FilterCardData(Mode mode, List<ItemStack> items, Set<ResourceLocation> tags, boolean strict) {

    /** Maximum number of items a single card can list. */
    public static final int MAX_ITEMS = 5;
    /** Maximum number of tags a single card can list. */
    public static final int MAX_TAGS = 3;

    public FilterCardData {
        // Defensive, immutable copies. Drop empties so ItemStack.CODEC never sees a blank stack.
        items = (items == null) ? List.of()
                : items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).limit(MAX_ITEMS).toList();
        tags = (tags == null) ? Set.of() : Set.copyOf(tags);
    }

    /** The three Filter Card modes; serialized name is used in both NBT and tooltips. */
    public enum Mode implements StringRepresentable {
        ITEM("item"),
        MOD("mod"),
        TAG("tag"),
        OVERFLOW("overflow");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Freshly-crafted, unconfigured card: Item mode with no targets. */
    public static final FilterCardData EMPTY = new FilterCardData(Mode.ITEM, List.of(), Set.of(), false);

    public static final Codec<FilterCardData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StringRepresentable.fromEnum(Mode::values).fieldOf("mode").forGetter(FilterCardData::mode),
            ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(FilterCardData::items),
            ResourceLocation.CODEC.listOf().optionalFieldOf("tags", List.of())
                    .xmap(list -> (Set<ResourceLocation>) new LinkedHashSet<>(list), List::copyOf)
                    .forGetter(FilterCardData::tags),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(FilterCardData::strict)
    ).apply(inst, FilterCardData::new));

    /** Registry-aware (ItemStack components need registry access on the network). */
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterCardData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private static ResourceLocation id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    /** Convert to the pure routing representation. */
    public FilterMode toFilterMode() {
        return switch (mode) {
            case ITEM -> strict
                    ? new FilterMode.ItemFilter(items.stream()
                            .map(s -> id(s) + " " + s.getComponentsPatch().toString()).collect(Collectors.toSet()), true)
                    : new FilterMode.ItemFilter(items.stream()
                            .map(s -> id(s).toString()).collect(Collectors.toSet()), false);
            case MOD -> new FilterMode.ModFilter(items.stream()
                    .map(s -> id(s).getNamespace()).collect(Collectors.toSet()));
            case TAG -> new FilterMode.TagFilter(tags.stream().map(ResourceLocation::toString).collect(Collectors.toSet()));
            case OVERFLOW -> new FilterMode.OverflowFilter();
        };
    }

    /** A copy with the mode replaced; the item/tag lists are kept so switching back loses nothing. */
    public FilterCardData withMode(Mode newMode) {
        return newMode == mode ? this : new FilterCardData(newMode, items, tags, strict);
    }

    /** Toggles strict (component-aware) item matching. */
    public FilterCardData withStrictToggled() {
        return new FilterCardData(mode, items, tags, !strict);
    }

    /**
     * Adds {@code stack} (as a single-count reference) to the item list. No-op (returns {@code this}) if an
     * equal item+components is already present or the list is at {@link #MAX_ITEMS}.
     */
    public FilterCardData withItemAdded(ItemStack stack) {
        if (stack.isEmpty() || items.size() >= MAX_ITEMS
                || items.stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, stack))) {
            return this;
        }
        List<ItemStack> next = new ArrayList<>(items);
        next.add(stack.copyWithCount(1));
        return new FilterCardData(mode, next, tags, strict);
    }

    /** Removes the item at {@code index}, or returns {@code this} if the index is out of range. */
    public FilterCardData withItemRemovedAt(int index) {
        if (index < 0 || index >= items.size()) {
            return this;
        }
        List<ItemStack> next = new ArrayList<>(items);
        next.remove(index);
        return new FilterCardData(mode, next, tags, strict);
    }

    /**
     * Toggles tag {@code id}: removes it if present, otherwise adds it (no-op when already at
     * {@link #MAX_TAGS}).
     */
    public FilterCardData withTagToggled(ResourceLocation id) {
        LinkedHashSet<ResourceLocation> next = new LinkedHashSet<>(tags);
        if (next.contains(id)) {
            next.remove(id);
        } else if (next.size() < MAX_TAGS) {
            next.add(id);
        } else {
            return this;
        }
        return new FilterCardData(mode, items, next, strict);
    }
}
