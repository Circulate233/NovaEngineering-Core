package github.kasuminova.novaeng.client.gl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The view frustum of the frame being drawn, so tile entity renderers can be culled against it.
 *
 * <p>Only whole chunks are culled by the renderer, so a renderer that sits in a visible chunk but behind the camera is
 * still drawn in full. Its bounding box being outside the view means nothing it could draw reaches the screen, so
 * skipping it changes nothing that can be seen.</p>
 *
 * <p>{@code ClippingHelperImpl.getInstance()} recomputes the frustum from the GL matrices every call - two matrix
 * readbacks - so it must not be asked per tile entity. Vanilla calls it exactly once per frame, after the camera
 * transform is set up, which is when the helper recorded here is captured; a frustum built from it carries the camera
 * position the rest of the frame is drawn with.</p>
 *
 * <p>Only the client render thread runs this.</p>
 */
public final class FrustumCache {

    private static Frustum frustum;

    private FrustumCache() {
    }

    /** Rebuilds the cached frustum from the helper the renderer has just built, at the camera it will be used with. */
    public static void refresh(final ClippingHelper helper) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final Entity view = minecraft.getRenderViewEntity();
        if (helper == null || view == null) {
            frustum = null;
            return;
        }
        final float partialTicks = minecraft.getRenderPartialTicks();
        final Frustum next = new Frustum(helper);
        next.setPosition(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * (double) partialTicks,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * (double) partialTicks,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double) partialTicks);
        frustum = next;
    }

    /** True when the box lies entirely outside the view, i.e. drawing it can have no visible effect. */
    public static boolean isOutside(final AxisAlignedBB box) {
        final Frustum current = frustum;
        return current != null && !current.isBoundingBoxInFrustum(box);
    }
}
