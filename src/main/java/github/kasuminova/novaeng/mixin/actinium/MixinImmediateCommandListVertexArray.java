package github.kasuminova.novaeng.mixin.actinium;

import com.mitchej123.lwjgl.LWJGLService;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Invalidates Nova's VAO shadow before Embeddium issues binds outside GLSM.
 */
@Mixin(targets = "dhj.embeddedt.embeddium.impl.gl.device.GLRenderDevice$ImmediateCommandList", remap = false)
public abstract class MixinImmediateCommandListVertexArray {

    @Redirect(method = {"bindVertexArray(Ldhj/embeddedt/embeddium/impl/gl/array/GlVertexArray;)V", "unbindVertexArray()V"},
        at = @At(value = "INVOKE", target = "Lcom/mitchej123/lwjgl/LWJGLService;glBindVertexArray(I)V"),
        remap = false, require = 2)
    private void nova$invalidateBeforeRawBind(final LWJGLService service, final int array) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
        service.glBindVertexArray(array);
    }
}
