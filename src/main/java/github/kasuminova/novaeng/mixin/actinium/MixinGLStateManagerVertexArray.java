package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses normalized same-VAO binds independently from generic attribute caching. */
@Mixin(value = GLStateManager.class, remap = false)
public abstract class MixinGLStateManagerVertexArray {

    @Redirect(method = "glBindVertexArray(I)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;bindVertexArray(I)V",
            remap = false),
        remap = false, require = 1)
    private static void nova$skipSameVertexArray(final RenderBackend backend,
                                                 final int normalizedVertexArray) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldBindVertexArray(normalizedVertexArray)) {
            backend.bindVertexArray(normalizedVertexArray);
            state.recordBoundVertexArray(normalizedVertexArray);
        }
    }

    @Inject(method = "glDeleteVertexArrays(I)V", at = @At("RETURN"),
        remap = false, require = 1)
    private static void nova$invalidateDeletedVertexArray(final int array, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "reset()V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$invalidateAfterReset(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "preInit(II)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$invalidateAfterPreInit(final int displayWidth,
                                                    final int displayHeight,
                                                    final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "init(Ljava/lang/Runnable;)V", at = @At("RETURN"),
        remap = false, require = 1)
    private static void nova$invalidateAfterInit(final Runnable initCallback, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "markSplashComplete()V", at = @At("RETURN"),
        remap = false, require = 1)
    private static void nova$invalidateAfterSplash(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }
}
