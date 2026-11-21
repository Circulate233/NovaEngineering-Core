package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLSMAttribStack;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tracks the normalized VAO restored directly by GLSM's state replay.
 */
@Mixin(value = GLSMAttribStack.class, remap = false)
public abstract class MixinGLSMAttribStackVertexArray {

    @Redirect(method = "replayStateToBackend()V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;bindVertexArray(I)V"),
        remap = false, require = 1)
    private static void nova$recordReplayedVertexArray(final RenderBackend backend, final int vertexArray) {
        backend.bindVertexArray(vertexArray);
        GenericAttributeStateImpl.instance().recordBoundVertexArray(vertexArray);
    }
}
