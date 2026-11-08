package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Same deferred attrib suppression as {@link MixinGLStateManagerGenericAttributes}, for the
 * Actinium versions that moved the flush body into the public GLSMCurrentAttribs facade.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.glsm.GLSMCurrentAttribs", remap = false)
public abstract class MixinGLSMCurrentAttribsGenericAttributes {

    @Redirect(method = "flushDeferredVertexAttribs()V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V",
            ordinal = 0,
            remap = false),
        remap = false, require = 0)
    private static void nova$flushColor(final RenderBackend backend, final int index,
                                        final float x, final float y, final float z, final float w) {
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
        remap = false, require = 0)
    private static void nova$flushSecondaryUv(final RenderBackend backend, final int index,
                                              final float x, final float y, final float z, final float w) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldUploadSecondaryUv(index, x, y, z, w)) {
            backend.vertexAttrib4f(index, x, y, z, w);
            state.recordAttribute(index, x, y, z, w);
        }
    }
}
