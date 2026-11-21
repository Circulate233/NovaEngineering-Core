package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLSMCurrentAttribs;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tracks setters and suppresses redundant deferred uploads at GLSM's shared implementation.
 */
@Mixin(value = GLSMCurrentAttribs.class, remap = false)
public abstract class MixinGLSMCurrentAttribsGenericAttributes {

    @Redirect(method = "flushDeferredVertexAttribs()V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V",
            ordinal = 0,
            remap = false),
        remap = false, require = 1)
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
        remap = false, require = 1)
    private static void nova$flushSecondaryUv(final RenderBackend backend, final int index,
                                              final float x, final float y, final float z, final float w) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldUploadSecondaryUv(index, x, y, z, w)) {
            backend.vertexAttrib4f(index, x, y, z, w);
            state.recordAttribute(index, x, y, z, w);
        }
    }

    @Redirect(method = {"glVertexAttrib2f(IFF)V", "glVertexAttrib2s(ISS)V"},
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib2f(IFF)V"),
        remap = false, require = 2)
    private static void nova$recordVertexAttrib2f(final RenderBackend backend, final int index,
                                                  final float x, final float y) {
        backend.vertexAttrib2f(index, x, y);
        GenericAttributeStateImpl.instance().recordAttribute(index, x, y, 0.0F, 1.0F);
    }

    @Redirect(method = "glVertexAttrib3f(IFFF)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib3f(IFFF)V"),
        remap = false, require = 1)
    private static void nova$recordVertexAttrib3f(final RenderBackend backend, final int index,
                                                  final float x, final float y, final float z) {
        backend.vertexAttrib3f(index, x, y, z);
        GenericAttributeStateImpl.instance().recordAttribute(index, x, y, z, 1.0F);
    }

    @Redirect(method = "glVertexAttrib4f(IFFFF)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V"),
        remap = false, require = 1)
    private static void nova$recordVertexAttrib4f(final RenderBackend backend, final int index,
                                                  final float x, final float y, final float z, final float w) {
        backend.vertexAttrib4f(index, x, y, z, w);
        GenericAttributeStateImpl.instance().recordAttribute(index, x, y, z, w);
    }
}
