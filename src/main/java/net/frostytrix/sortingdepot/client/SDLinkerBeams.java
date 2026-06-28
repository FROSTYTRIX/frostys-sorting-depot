package net.frostytrix.sortingdepot.client;

import java.util.OptionalDouble;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.sortingdepot.Config;
import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.item.LinkerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only world overlay drawn at {@code AFTER_TRANSLUCENT_BLOCKS} using immediate-mode lines:
 * <ul>
 *   <li>(while holding a Linker) the selected node is outlined with a short vertical beam;</li>
 *   <li>(while holding a Linker, off by default) a wire from every nearby Controller to each linked node;</li>
 *   <li>(triggered by clicking a row in the Depot Terminal) a transient outline around the corresponding
 *       Linker Node, drawn through walls via a custom no-depth-test render type.</li>
 * </ul>
 * Colour/toggles come from the client {@link Config}. Line widths are fixed by {@code RenderType.lines}
 * on 1.21.x, so the width settings only take effect on 26.2.
 */
public final class SDLinkerBeams {

    private static final int WIRING_CHUNK_RADIUS = 4;

    /**
     * Custom render type: a clone of {@link RenderType#lines()} with depth-testing disabled, so the
     * Terminal click-to-highlight outline draws through walls.
     */
    private static final RenderType LINES_NO_DEPTH = RenderType.create(
            "frostyssortingdepot_lines_no_depth",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .createCompositeState(false));

    /** Session override for the wiring overlay: {@code null} = use the config default. Toggled by keybind. */
    private static Boolean wiringOverride;

    /** Transient highlight requested by the Terminal: outline this node until {@code System.currentTimeMillis()} passes the expiry. */
    private static @Nullable BlockPos highlightPos;
    private static long highlightExpiresAtMillis;

    /** Asks the world overlay to outline {@code pos} in cyan for {@code durationMillis}. Click-from-Terminal entry point. */
    public static void highlight(BlockPos pos, long durationMillis) {
        highlightPos = pos.immutable();
        highlightExpiresAtMillis = System.currentTimeMillis() + durationMillis;
    }

    private SDLinkerBeams() {
    }

    public static boolean wiringActive() {
        return wiringOverride != null ? wiringOverride : Config.SHOW_WIRING.get();
    }

    public static void toggleWiring() {
        wiringOverride = !wiringActive();
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        int color = Config.beamColorArgb();
        boolean holdsLinker = holdsLinker(mc.player);
        BlockPos activeHighlight = highlightPos != null && System.currentTimeMillis() < highlightExpiresAtMillis
                ? highlightPos
                : null;
        if (activeHighlight == null) {
            highlightPos = null;
        }
        if (!holdsLinker && activeHighlight == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = poseStack.last();

        // Normal (depth-tested) pass: held-linker beam + wiring.
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        if (holdsLinker && Config.SHOW_BEAM.get()) {
            BlockPos selected = selectedNode(mc.player);
            if (selected != null) {
                box(vc, pose, new AABB(selected), color);
                Vec3 base = Vec3.atCenterOf(selected);
                line(vc, pose, base, base.add(0.0, 2.5, 0.0), color);
            }
        }
        if (holdsLinker && wiringActive()) {
            drawWiring(mc, vc, pose, color);
        }
        buffers.endBatch(RenderType.lines());

        // X-ray pass for the Terminal click-to-highlight outline (LINES_NO_DEPTH disables depth testing
        // so the box and beam render through walls).
        if (activeHighlight != null) {
            VertexConsumer xrayVc = buffers.getBuffer(LINES_NO_DEPTH);
            box(xrayVc, pose, new AABB(activeHighlight), color);
            Vec3 base = Vec3.atCenterOf(activeHighlight);
            line(xrayVc, pose, base, base.add(0.0, 2.5, 0.0), color);
            buffers.endBatch(LINES_NO_DEPTH);
        }

        poseStack.popPose();
    }

    private static void drawWiring(Minecraft mc, VertexConsumer vc, PoseStack.Pose pose, int color) {
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
                            line(vc, pose, from, Vec3.atCenterOf(node), color);
                        }
                    }
                }
            }
        }
    }

    /** Draws a single coloured line segment. */
    private static void line(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, int color) {
        float nx = (float) (b.x - a.x);
        float ny = (float) (b.y - a.y);
        float nz = (float) (b.z - a.z);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-4F) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(color).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(color).setNormal(pose, nx, ny, nz);
    }

    /** Draws the 12 edges of a box. */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, AABB a, int color) {
        Vec3 c000 = new Vec3(a.minX, a.minY, a.minZ);
        Vec3 c100 = new Vec3(a.maxX, a.minY, a.minZ);
        Vec3 c101 = new Vec3(a.maxX, a.minY, a.maxZ);
        Vec3 c001 = new Vec3(a.minX, a.minY, a.maxZ);
        Vec3 c010 = new Vec3(a.minX, a.maxY, a.minZ);
        Vec3 c110 = new Vec3(a.maxX, a.maxY, a.minZ);
        Vec3 c111 = new Vec3(a.maxX, a.maxY, a.maxZ);
        Vec3 c011 = new Vec3(a.minX, a.maxY, a.maxZ);
        line(vc, pose, c000, c100, color);
        line(vc, pose, c100, c101, color);
        line(vc, pose, c101, c001, color);
        line(vc, pose, c001, c000, color);
        line(vc, pose, c010, c110, color);
        line(vc, pose, c110, c111, color);
        line(vc, pose, c111, c011, color);
        line(vc, pose, c011, c010, color);
        line(vc, pose, c000, c010, color);
        line(vc, pose, c100, c110, color);
        line(vc, pose, c101, c111, color);
        line(vc, pose, c001, c011, color);
    }

    private static boolean holdsLinker(Player player) {
        return player.getMainHandItem().getItem() instanceof LinkerItem
                || player.getOffhandItem().getItem() instanceof LinkerItem;
    }

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
