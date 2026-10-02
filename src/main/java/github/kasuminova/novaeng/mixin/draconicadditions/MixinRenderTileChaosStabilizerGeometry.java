package github.kasuminova.novaeng.mixin.draconicadditions;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.render.CclModelGeometryCache;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The same treatment as the reactor core, for the chaos stabiliser core, which is built the same way: one CodeChicken
 * Lib model, drawn every frame with a spin transform as its only operation and a shader carrying the rest.
 *
 * @see github.kasuminova.novaeng.mixin.draconicevolution.MixinRenderTileReactorCoreGeometry
 */
@Mixin(targets = "net.foxmcloud.draconicadditions.client.render.tile.RenderTileChaosStabilizerCore",
    remap = false)
public abstract class MixinRenderTileChaosStabilizerGeometry {

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
    private static void nova$replayOrCapture(final CCModel model, final CCRenderState state,
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
