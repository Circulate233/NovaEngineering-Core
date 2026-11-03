package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Same vertex array suppression as {@link MixinGLStateManagerVertexArray}, for the Actinium
 * versions that moved the bind body into the public GLSMBuffers facade. Targets are resolved by
 * name so the mixin is skipped on versions without that class.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.glsm.GLSMVertexInput", remap = false)
public abstract class MixinGLSMVertexInputVertexArray {

    @Redirect(method = "glBindVertexArray(I)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;bindVertexArray(I)V",
            remap = false),
        remap = false, require = 0)
    private static void nova$skipSameVertexArray(final RenderBackend backend,
                                                 final int normalizedVertexArray) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        if (state.shouldBindVertexArray(normalizedVertexArray)) {
            backend.bindVertexArray(normalizedVertexArray);
            state.recordBoundVertexArray(normalizedVertexArray);
        }
    }
}
