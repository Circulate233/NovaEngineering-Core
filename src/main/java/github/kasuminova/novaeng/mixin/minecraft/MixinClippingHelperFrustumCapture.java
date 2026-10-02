package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.gl.FrustumCache;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the frustum the renderer builds for the frame, so tile entity renderers can be culled against it.
 *
 * <p>{@code getInstance()} rebuilds the frustum from the current GL matrices and is called once per frame by the
 * renderer, right after the camera transform is set up. Reading it here is what makes the per tile entity test in
 * {@code MixinTileEntityRendererDistance} affordable: it reuses this one capture instead of taking a matrix readback
 * per block entity.</p>
 */
@Mixin(value = ClippingHelperImpl.class, remap = false)
public abstract class MixinClippingHelperFrustumCapture {

    @Inject(method = "getInstance()Lnet/minecraft/client/renderer/culling/ClippingHelper;",
            at = @At("RETURN"), remap = true, require = 1)
    private static void nova$captureFrustum(final CallbackInfoReturnable<ClippingHelper> cir) {
        if (NovaEngCoreConfig.CLIENT.optimizeTesrFrustumCulling) {
            FrustumCache.refresh(cir.getReturnValue());
        }
    }
}
