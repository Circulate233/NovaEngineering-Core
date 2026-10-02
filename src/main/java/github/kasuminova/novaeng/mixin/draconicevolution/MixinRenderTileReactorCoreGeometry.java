package github.kasuminova.novaeng.mixin.draconicevolution;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileReactorCore;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.render.CclModelGeometryCache;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Draws the reactor core's model from remembered vertices, turning the per-frame transform into a GL matrix.
 *
 * <p>The renderer builds a transform - the core's spin, its scale and the half-block offset - hands it to CodeChicken
 * Lib as the only operation, and lets the library apply it to every vertex while it writes them. The vertices are
 * constant, so the transform is applied to the GL matrix instead and the vertex data is remembered and replayed:
 * the six calls the library makes per vertex become one {@code addVertexData}, and nothing per-vertex is computed at
 * all. The shader the renderer already uses reads the standard matrices, so it follows the GL matrix on its own.</p>
 *
 * <p>The transform is pushed before the model is written and popped when the renderer draws, which are the two calls
 * that always run in that order - there is no separate exit to leak a pushed matrix through.</p>
 */
@Mixin(value = RenderTileReactorCore.class, remap = false)
public abstract class MixinRenderTileReactorCoreGeometry {

    /** The operation handed to the library becomes a no-op: the transform has already been moved to the GL matrix. */
    @Unique
    private static final Matrix4 NOVA$IDENTITY = new Matrix4();

    @Redirect(
        method = "renderCore(DDDFFFDZ)V",
        at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/render/RenderUtils;getMatrix(Lcodechicken/lib/vec/Vector3;Lcodechicken/lib/vec/Rotation;D)Lcodechicken/lib/vec/Matrix4;"),
        remap = false, require = 1
    )
    private static Matrix4 nova$moveTransformToGlMatrix(final Vector3 center, final Rotation rotation,
                                                        final double scale) {
        final Matrix4 matrix = RenderUtils.getMatrix(center, rotation, scale);
        if (!NovaEngCoreConfig.CLIENT.optimizeDraconicModelGeometry) {
            return matrix;
        }
        GlStateManager.pushMatrix();
        matrix.glApply();
        return NOVA$IDENTITY;
    }

    @Redirect(
        method = "renderCore(DDDFFFDZ)V",
        at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/render/CCModel;render(Lcodechicken/lib/render/CCRenderState;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"),
        remap = false, require = 1
    )
    private static void nova$replayOrCapture(final codechicken.lib.render.CCModel model, final CCRenderState state,
                                              final IVertexOperation[] ops) {
        if (NovaEngCoreConfig.CLIENT.optimizeDraconicModelGeometry
                && CclModelGeometryCache.replay(model, state, ops)) {
            return;
        }
        model.render(state, ops);
        if (NovaEngCoreConfig.CLIENT.optimizeDraconicModelGeometry) {
            CclModelGeometryCache.remember(model, state);
        }
    }

    @Redirect(
        method = "renderCore(DDDFFFDZ)V",
        at = @At(value = "INVOKE", target = "Lcodechicken/lib/render/CCRenderState;draw()V"),
        remap = false, require = 1
    )
    private static void nova$drawAndRestoreMatrix(final CCRenderState state) {
        state.draw();
        if (NovaEngCoreConfig.CLIENT.optimizeDraconicModelGeometry) {
            GlStateManager.popMatrix();
        }
    }
}
