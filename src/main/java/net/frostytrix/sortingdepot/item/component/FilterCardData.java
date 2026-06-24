package net.frostytrix.sortingdepot.item.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.frostytrix.sortingdepot.routing.FilterMode;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * The data component stored on a Filter Card. This is the Minecraft-facing mirror of the pure
 * {@link FilterMode}: it holds {@link Identifier}s and serializes with Minecraft codecs, then
 * {@linkplain #toFilterMode() converts} to the Minecraft-free routing type at the adapter boundary.
 *
 * <p>A card holds <strong>up to {@value #MAX_ITEMS} items</strong> and <strong>up to {@value #MAX_TAGS}
 * tags</strong>. The active {@link Mode} decides which set is consulted when routing: {@code ITEM} uses
 * {@link #items}, {@code TAG} uses {@link #tags}, {@code OVERFLOW} matches everything. Both lists are
 * retained across mode switches so the GUI can populate the tag checklist from the chosen items.
 *
 * @param mode  which matching strategy this card uses
 * @param items target item ids for {@link Mode#ITEM}
 * @param tags  tag keys for {@link Mode#TAG}
 */
public record FilterCardData(Mode mode, List<Identifier> items, Set<Identifier> tags) {

    /** Maximum number of exact items a single card can list. */
    public static final int MAX_ITEMS = 5;
    /** Maximum number of tags a single card can list. */
    public static final int MAX_TAGS = 3;

    public FilterCardData {
        items = (items == null) ? List.of() : List.copyOf(items);
        tags = (tags == null) ? Set.of() : Set.copyOf(tags);
    }

    /** The three Filter Card modes; serialized name is used in both NBT and tooltips. */
    public enum Mode implements StringRepresentable {
        ITEM("item"),
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
    public static final FilterCardData EMPTY = new FilterCardData(Mode.ITEM, List.of(), Set.of());

    public static final Codec<FilterCardData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StringRepresentable.fromEnum(Mode::values).fieldOf("mode").forGetter(FilterCardData::mode),
            Identifier.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(FilterCardData::items),
            Identifier.CODEC.listOf().optionalFieldOf("tags", List.of())
                    .xmap(list -> (Set<Identifier>) new LinkedHashSet<>(list), List::copyOf)
                    .forGetter(FilterCardData::tags)
    ).apply(inst, FilterCardData::new));

    /** Derived from {@link #CODEC} via NBT — avoids hand-writing a composite stream codec. */
    public static final StreamCodec<ByteBuf, FilterCardData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Convert to the pure routing representation. Identifiers become plain strings. */
    public FilterMode toFilterMode() {
        return switch (mode) {
            case ITEM -> new FilterMode.ItemFilter(items.stream().map(Identifier::toString).collect(Collectors.toSet()));
            case TAG -> new FilterMode.TagFilter(tags.stream().map(Identifier::toString).collect(Collectors.toSet()));
            case OVERFLOW -> new FilterMode.OverflowFilter();
        };
    }

    /** A copy with the mode replaced; the item/tag lists are kept so switching back loses nothing. */
    public FilterCardData withMode(Mode newMode) {
        return new FilterCardData(newMode, items, tags);
    }

    /**
     * Adds {@code id} to the item list. No-op (returns {@code this}) if it is already present or the list
     * is already at {@link #MAX_ITEMS}.
     */
    public FilterCardData withItemAdded(Identifier id) {
        if (items.contains(id) || items.size() >= MAX_ITEMS) {
            return this;
        }
        List<Identifier> next = new ArrayList<>(items);
        next.add(id);
        return new FilterCardData(mode, next, tags);
    }

    /** Removes the item at {@code index}, or returns {@code this} if the index is out of range. */
    public FilterCardData withItemRemovedAt(int index) {
        if (index < 0 || index >= items.size()) {
            return this;
        }
        List<Identifier> next = new ArrayList<>(items);
        next.remove(index);
        return new FilterCardData(mode, next, tags);
    }

    /**
     * Toggles tag {@code id}: removes it if present, otherwise adds it (no-op when already at
     * {@link #MAX_TAGS}). Insertion order is preserved so the GUI checklist stays stable.
     */
    public FilterCardData withTagToggled(Identifier id) {
        LinkedHashSet<Identifier> next = new LinkedHashSet<>(tags);
        if (next.contains(id)) {
            next.remove(id);
        } else if (next.size() < MAX_TAGS) {
            next.add(id);
        } else {
            return this;
        }
        return new FilterCardData(mode, items, next);
    }
}
