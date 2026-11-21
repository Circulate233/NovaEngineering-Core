package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates generic attribute shadows across GLSM initialization and context changes.
 */
@Mixin(value = GLStateManager.class, remap = false)
public abstract class MixinGLStateManagerGenericAttributes {

    @Inject(method = {"reset()V", "recreateDefaultVertexArray()V"},
        at = @At("RETURN"), remap = false, require = 1)
    private static void nova$invalidateAfterReset(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateAttributes();
    }

    @Inject(method = "preInit(II)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$invalidateAfterPreInit(final int displayWidth,
                                                    final int displayHeight,
                                                    final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateAttributes();
    }

    @Inject(method = "init(Ljava/lang/Runnable;)V", at = @At("RETURN"),
        remap = false, require = 1)
    private static void nova$invalidateAfterInit(final Runnable initCallback, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateAttributes();
    }

    @Inject(method = "markSplashComplete()V", at = @At("RETURN"),
        remap = false, require = 1)
    private static void nova$invalidateAfterSplash(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateAttributes();
    }

    @ModifyReturnValue(method = "bindThreadContext(Lcom/gtnewhorizons/angelica/glsm/GLContextState;)Z",
        at = @At("RETURN"), remap = false, require = 1)
    private static boolean nova$invalidateAfterContextChange(final boolean changed) {
        // A display context can change its native handle while retaining the same GLSM state.
        GenericAttributeStateImpl.instance().invalidateAttributes();
        return changed;
    }
}
