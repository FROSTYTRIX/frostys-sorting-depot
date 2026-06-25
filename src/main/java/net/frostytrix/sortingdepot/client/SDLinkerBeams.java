package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.Config;
import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.item.LinkerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only world overlay drawn while the player holds a Linker, using the 26.2 {@code Gizmos}
 * debug-draw API (which renders in normal play):
 * <ul>
 *   <li>the Linker's currently-selected node is outlined with a short vertical beam (always, if enabled);</li>
 *   <li>optionally (off by default), a wire from every nearby Controller to each of its linked nodes.</li>
 * </ul>
 * Colours/widths/toggles come from the client {@link Config}.
 */
public final class SDLinkerBeams {

    /** Chunk radius around the player searched for Controllers when drawing wiring. */
    private static final int WIRING_CHUNK_RADIUS = 4;

    private static boolean warnedNoCollector;

    /** Session override for the wiring overlay: {@code null} = use the config default. Toggled by keybind. */
    private static Boolean wiringOverride;

    private SDLinkerBeams() {
    }

    /** Whether the Controller → node wiring overlay is currently shown (keybind override, else config). */
    public static boolean wiringActive() {
        return wiringOverride != null ? wiringOverride : Config.SHOW_WIRING.get();
    }

    /** Flips the wiring overlay for this session (called from the keybind). */
    public static void toggleWiring() {
        wiringOverride = !wiringActive();
    }

    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !holdsLinker(mc.player)) {
            return;
        }
        int color = Config.beamColorArgb();
        try {
            if (Config.SHOW_BEAM.get()) {
                BlockPos selected = selectedNode(mc.player);
                if (selected != null) {
                    Gizmos.cuboid(selected, GizmoStyle.stroke(color, Config.outlineWidth()));
                    Vec3 base = Vec3.atCenterOf(selected);
                    Gizmos.line(base, base.add(0.0, 2.5, 0.0), color, Config.beamWidth());
                }
            }
            if (wiringActive()) {
                drawWiring(mc, color, Config.beamWidth());
            }
        } catch (IllegalStateException noCollector) {
            // No gizmo collector active at this render stage — log once so we can move the hook.
            if (!warnedNoCollector) {
                warnedNoCollector = true;
                FrostysSortingDepot.LOGGER.warn("Linker beam: no gizmo collector at AfterTranslucentBlocks", noCollector);
            }
        }
    }

    /** Draws a wire from each loaded Controller near the player to each of its registered nodes. */
    private static void drawWiring(Minecraft mc, int color, float width) {
        Level level = mc.level;
        BlockPos p = mc.player.blockPosition();
        int pcx = p.getX() >> 4;
        int pcz = p.getZ() >> 4;
        for (int dx = -WIRING_CHUNK_RADIUS; dx <= WIRING_CHUNK_RADIUS; dx++) {
            for (int dz = -WIRING_CHUNK_RADIUS; dz <= WIRING_CHUNK_RADIUS; dz++) {
                LevelChunk chunk = level.getChunk(pcx + dx, pcz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof DepotControllerBlockEntity controller) {
                        Vec3 from = Vec3.atCenterOf(controller.getBlockPos());
                        for (BlockPos node : controller.getLinkers()) {
                            Gizmos.line(from, Vec3.atCenterOf(node), color, width);
                        }
                    }
                }
            }
        }
    }

    private static boolean holdsLinker(Player player) {
        return player.getMainHandItem().getItem() instanceof LinkerItem
                || player.getOffhandItem().getItem() instanceof LinkerItem;
    }

    /** The Linker Node currently selected by a held Linker (main or off hand), or {@code null}. */
    private static @Nullable BlockPos selectedNode(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof LinkerItem) {
                BlockPos pos = LinkerItem.linkedPos(stack);
                if (pos != null) {
                    return pos;
                }
            }
        }
        return null;
    }
}
