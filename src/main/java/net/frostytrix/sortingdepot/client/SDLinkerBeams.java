package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.item.LinkerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only world overlay drawn while the player holds a Linker: highlights the currently-selected
 * Linker Node (so you can see what a later Controller click will register) using the 26.2 {@code Gizmos}
 * debug-draw API, which renders in normal play.
 *
 * <p>First iteration: only the held Linker's selected node (data lives on the item component, so no
 * block-entity sync is needed). Controller → node wiring beams come next, once this hook is confirmed.
 */
public final class SDLinkerBeams {

    /** Frosty cyan/teal accent. */
    private static final int COLOR = 0xFF33CCCC;

    private static boolean warnedNoCollector;

    private SDLinkerBeams() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos selected = selectedNode(mc.player);
        if (selected == null) {
            return;
        }
        try {
            Gizmos.cuboid(selected, GizmoStyle.stroke(COLOR));
            Vec3 base = Vec3.atCenterOf(selected);
            Gizmos.line(base, base.add(0.0, 2.5, 0.0), COLOR, 3.0F);
        } catch (IllegalStateException noCollector) {
            // No gizmo collector active at this render stage — log once so we can move the hook.
            if (!warnedNoCollector) {
                warnedNoCollector = true;
                FrostysSortingDepot.LOGGER.warn("Linker beam: no gizmo collector at AfterTranslucentBlocks", noCollector);
            }
        }
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
