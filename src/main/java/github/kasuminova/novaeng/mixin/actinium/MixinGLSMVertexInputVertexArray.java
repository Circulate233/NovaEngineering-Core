package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLSMVertexInput;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses normalized same-VAO binds and tracks deletion in GLSM's shared implementation.
 */
@Mixin(value = GLSMVertexInput.class, remap = false)
public abstract class MixinGLSMVertexInputVertexArray {

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

    @Redirect(method = "glDeleteVertexArrays(I)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;deleteVertexArrays(I)V"),
        remap = false, require = 1)
    private static void nova$invalidateDeletedVertexArray(final RenderBackend backend, final int array) {
        backend.deleteVertexArrays(array);
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }
}
