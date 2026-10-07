package github.kasuminova.novaeng.client.render;

import com.dhj.actinium.shadows.ShadowRenderingState;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * Uses the caller's camera only during world entity rendering, including nested portal renders.
 */
public final class TileEntityRenderCulling {
    private static final ThreadLocal<PassStack> PASSES = ThreadLocal.withInitial(PassStack::new);

    private TileEntityRenderCulling() {
    }

    public static void begin(final Entity view, final ICamera camera, final float partialTicks) {
        final PassStack stack = PASSES.get();
        if (stack.depth == stack.passes.size()) {
            stack.passes.add(new Pass());
        }
        final Pass pass = stack.passes.get(stack.depth++);
        // A main-camera distance gate must never remove a shadow caster.
        if (view == null || camera == null || isShadowPass()) {
            return;
        }
        pass.world = view.world;
        pass.camera = camera;
        pass.x = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        pass.y = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        pass.z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
    }

    public static void end() {
        final PassStack stack = PASSES.get();
        final Pass pass = stack.passes.get(--stack.depth);
        pass.camera = null;
        pass.world = null;
        pass.approvedTile = null;
        pass.preparedRendererTile = null;
        pass.preparedRenderer = null;
        pass.approvalDepth = 0;
    }

    public static boolean shouldSkip(final TileEntity tile) {
        final Pass pass = current();
        if (pass == null || pass.camera == null || pass.approvedTile == tile || tile.getWorld() != pass.world) {
            return false;
        }
        final boolean distance = NovaEngCoreConfig.CLIENT.optimizeTesrRenderDistance
            && NovaEngCoreConfig.CLIENT.tesrRenderDistance > 0;
        final boolean frustum = NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling;
        if (!distance && !frustum) {
            return false;
        }
        final AxisAlignedBB box = tile.getRenderBoundingBox();
        if (box == null || box == TileEntity.INFINITE_EXTENT_AABB
            || !TileEntityVisibility.hasFiniteBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) {
            return false;
        }
        final boolean rejected = (frustum && !pass.camera.isBoundingBoxInFrustum(box))
            || (distance && TileEntityVisibility.isBeyondDistance(box.minX, box.minY, box.minZ,
            box.maxX, box.maxY, box.maxZ, pass.x, pass.y, pass.z, NovaEngCoreConfig.CLIENT.tesrRenderDistance));
        if (!rejected) {
            return false;
        }
        // Global renderers can intentionally draw effects outside their advertised finite volume.
        final TileEntitySpecialRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(tile);
        if (renderer != null && !renderer.isGlobalRenderer(tile)) {
            return true;
        }
        pass.preparedRendererTile = tile;
        pass.preparedRenderer = renderer;
        return false;
    }

    /**
     * Avoids a second bounds query when a pre-checked Actinium call enters the dispatcher.
     */
    public static Approval approve(final TileEntity tile) {
        final Pass pass = current();
        if (pass == null) {
            return null;
        }
        if (pass.approvalDepth == pass.approvals.size()) {
            pass.approvals.add(new Approval());
        }
        final Approval previous = pass.approvals.get(pass.approvalDepth++);
        previous.pass = pass;
        previous.approvedTile = pass.approvedTile;
        previous.preparedRendererTile = pass.preparedRendererTile;
        previous.preparedRenderer = pass.preparedRenderer;
        pass.approvedTile = tile;
        return previous;
    }

    public static void restoreApproval(final Approval previous) {
        if (previous != null) {
            final Pass pass = previous.pass;
            pass.approvedTile = previous.approvedTile;
            pass.preparedRendererTile = previous.preparedRendererTile;
            pass.preparedRenderer = previous.preparedRenderer;
            pass.approvalDepth--;
        }
    }

    public static TileEntitySpecialRenderer<TileEntity> consumePreparedRenderer(
        final TileEntityRendererDispatcher dispatcher, final TileEntity tile
    ) {
        final Pass pass = current();
        if (pass != null && pass.preparedRendererTile == tile) {
            final TileEntitySpecialRenderer<TileEntity> renderer = pass.preparedRenderer;
            pass.preparedRendererTile = null;
            pass.preparedRenderer = null;
            return renderer;
        }
        return dispatcher.getRenderer(tile);
    }

    private static Pass current() {
        final PassStack stack = PASSES.get();
        return stack.depth == 0 ? null : stack.passes.get(stack.depth - 1);
    }

    private static boolean isShadowPass() {
        return MixinDecisions.actiniumLoaded && ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    private static final class PassStack {
        private final ObjectArrayList<Pass> passes = new ObjectArrayList<>(2);
        private int depth;
    }

    private static final class Pass {
        private ICamera camera;
        private World world;
        private TileEntity approvedTile;
        private TileEntity preparedRendererTile;
        private TileEntitySpecialRenderer<TileEntity> preparedRenderer;
        private final ObjectArrayList<Approval> approvals = new ObjectArrayList<>(2);
        private int approvalDepth;
        private double x;
        private double y;
        private double z;
    }

    public static final class Approval {
        private Pass pass;
        private TileEntity approvedTile;
        private TileEntity preparedRendererTile;
        private TileEntitySpecialRenderer<TileEntity> preparedRenderer;
    }
}
