package net.frostytrix.sortingdepot.item.component;

import java.util.List;
import java.util.Optional;
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
 * @param mode   which matching strategy this card uses
 * @param itemId target item for {@link Mode#ITEM}, empty when unconfigured
 * @param tags   tag keys for {@link Mode#TAG}
 * @param strict whether {@link Mode#ITEM} also compares data components
 */
public record FilterCardData(Mode mode, Optional<Identifier> itemId, Set<Identifier> tags, boolean strict) {

    public FilterCardData {
        itemId = (itemId == null) ? Optional.empty() : itemId;
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

    /** Freshly-crafted, unconfigured card: Item mode with no target. */
    public static final FilterCardData EMPTY = new FilterCardData(Mode.ITEM, Optional.empty(), Set.of(), false);

    public static final Codec<FilterCardData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StringRepresentable.fromEnum(Mode::values).fieldOf("mode").forGetter(FilterCardData::mode),
            Identifier.CODEC.optionalFieldOf("item_id").forGetter(FilterCardData::itemId),
            Identifier.CODEC.listOf().optionalFieldOf("tags", List.of())
                    .xmap(list -> (Set<Identifier>) Set.copyOf(list), set -> List.copyOf(set))
                    .forGetter(FilterCardData::tags),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(FilterCardData::strict)
    ).apply(inst, FilterCardData::new));

    /** Derived from {@link #CODEC} via NBT — avoids hand-writing a composite stream codec. */
    public static final StreamCodec<ByteBuf, FilterCardData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Convert to the pure routing representation. Identifiers become plain strings. */
    public FilterMode toFilterMode() {
        return switch (mode) {
            // An unconfigured Item card (no target) yields a filter that matches nothing.
            case ITEM -> new FilterMode.ItemFilter(itemId.map(Identifier::toString).orElse(""), null, strict);
            case TAG -> new FilterMode.TagFilter(tags.stream().map(Identifier::toString).collect(Collectors.toSet()));
            case OVERFLOW -> new FilterMode.OverflowFilter();
        };
    }

    /** Returns the next mode in the cycle ITEM → TAG → OVERFLOW → ITEM. */
    public Mode nextMode() {
        Mode[] values = Mode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }

    /** A copy with the mode replaced (payload retained so a later cycle back doesn't lose it). */
    public FilterCardData withMode(Mode newMode) {
        return new FilterCardData(newMode, itemId, tags, strict);
    }

    /** A configured Item-mode card targeting the given item id. */
    public static FilterCardData ofItem(Identifier id, boolean strict) {
        return new FilterCardData(Mode.ITEM, Optional.of(id), Set.of(), strict);
    }
}
