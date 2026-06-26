package net.frostytrix.sortingdepot;

import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side ({@code COMMON}) tunables: transfer rate, batch size, network size, and an optional
 * block-tag whitelist that constrains where Linker Nodes are allowed to push items. On a dedicated
 * server, the server's values take effect for the world.
 */
public final class CommonConfig {

    private CommonConfig() {
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TRANSFER_COOLDOWN = BUILDER
            .comment("Ticks between Controller transfer cycles. Lower is faster; a vanilla hopper uses 8."
                    + " The default of 2 is roughly 4x a hopper.")
            .defineInRange("controller.transferCooldown", 2, 1, 40);

    public static final ModConfigSpec.IntValue BATCH_SIZE = BUILDER
            .comment("How many items the Controller can move per cycle. 1 matches hopper feel; raise for"
                    + " heavy throughput in modpacks. Capped at 64 to keep one cycle bounded.")
            .defineInRange("controller.batchSize", 1, 1, 64);

    public static final ModConfigSpec.IntValue MAX_NETWORK_SIZE = BUILDER
            .comment("Hard cap on the number of Linker Nodes a single Controller can have registered.")
            .defineInRange("controller.maxNetworkSize", 64, 1, 1024);

    public static final ModConfigSpec.ConfigValue<String> VALID_DESTINATION_TAG = BUILDER
            .comment("Optional block tag (e.g. \"c:chests\"). When non-empty, Linker Nodes will only route"
                    + " into blocks in this tag — useful in modpacks to keep items out of hoppers/pipes by"
                    + " accident. Leave empty to allow any block exposing the item-handler capability.")
            .define("controller.validDestinationTag", "", CommonConfig::isTagIdOrEmpty);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean isTagIdOrEmpty(Object value) {
        if (!(value instanceof String s)) {
            return false;
        }
        return s.isEmpty() || ResourceLocation.tryParse(s) != null;
    }

    /** The configured destination tag, or {@link Optional#empty()} when the whitelist is disabled. */
    public static Optional<TagKey<Block>> validDestinationTag() {
        String raw = VALID_DESTINATION_TAG.get();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null ? Optional.empty() : Optional.of(TagKey.create(Registries.BLOCK, id));
    }
}
