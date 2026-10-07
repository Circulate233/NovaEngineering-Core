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

/**
 * Suppresses redundant GLSM COLOR and SECONDARY_UV constant uploads.
 */
@Mixin(value = GLStateManager.class, remap = false)
public abstract class MixinGLStateManagerGenericAttributes {

    @Redirect(method = "flushDeferredVertexAttribs()V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V",
            ordinal = 0,
            remap = false),
        remap = false, require = 1)
    private static void nova$flushColor(final RenderBackend backend,
                                        final int index,
                                        final float x,
                                        final float y,
                                        final float z,
                                        final float w) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldUploadColor(index, x, y, z, w)) {
            backend.vertexAttrib4f(index, x, y, z, w);
            state.recordAttribute(index, x, y, z, w);
        }
    }

    @Redirect(method = "flushDeferredVertexAttribs()V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V",
            ordinal = 1,
            remap = false),
        remap = false, require = 1)
    private static void nova$flushSecondaryUv(final RenderBackend backend,
                                              final int index,
                                              final float x,
                                              final float y,
                                              final float z,
                                              final float w) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldUploadSecondaryUv(index, x, y, z, w)) {
            backend.vertexAttrib4f(index, x, y, z, w);
            state.recordAttribute(index, x, y, z, w);
        }
    }

    @Inject(method = "glVertexAttrib2f(IFF)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$recordVertexAttrib2f(final int index,
                                                  final float v0,
                                                  final float v1,
                                                  final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().recordAttribute(index, v0, v1, 0.0F, 1.0F);
    }

    @Inject(method = "glVertexAttrib2s(ISS)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$recordVertexAttrib2s(final int index,
                                                  final short v0,
                                                  final short v1,
                                                  final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().recordAttribute(index, v0, v1, 0.0F, 1.0F);
    }

    @Inject(method = "glVertexAttrib3f(IFFF)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$recordVertexAttrib3f(final int index,
                                                  final float v0,
                                                  final float v1,
                                                  final float v2,
                                                  final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().recordAttribute(index, v0, v1, v2, 1.0F);
    }

    @Inject(method = "glVertexAttrib4f(IFFFF)V", at = @At("RETURN"), remap = false, require = 1)
    private static void nova$recordVertexAttrib4f(final int index,
                                                  final float v0,
                                                  final float v1,
                                                  final float v2,
                                                  final float v3,
                                                  final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().recordAttribute(index, v0, v1, v2, v3);
    }

    @Inject(method = "reset()V", at = @At("RETURN"), remap = false, require = 1)
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
}
