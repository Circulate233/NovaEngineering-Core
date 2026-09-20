package github.kasuminova.novaeng.mixin.actinium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses normalized same-VAO binds independently from generic attribute caching. */
@Mixin(value = GLStateManager.class, remap = false)
public abstract class MixinGLStateManagerVertexArray {

    @WrapOperation(method = "glBindVertexArray(I)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;bindVertexArray(I)V",
            remap = false),
        remap = false, require = 1)
    private static void nova$skipSameVertexArray(@Coerce final Object backend,
                                                 final int normalizedVertexArray,
                                                 final Operation<Void> original) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldBindVertexArray(normalizedVertexArray)) {
            original.call(backend, normalizedVertexArray);
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
