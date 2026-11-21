package github.kasuminova.novaeng.mixin.actinium;

import com.mitchej123.glsm.impl.PassThroughGLStateManager;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Invalidates the VAO shadow before the pass-through GL service changes the raw LWJGL binding.
 */
@Mixin(value = PassThroughGLStateManager.class, remap = false)
public abstract class MixinPassThroughGLStateManagerVertexArray {

    @Redirect(method = "glBindVertexArray(I)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindVertexArray(I)V"),
        remap = false, require = 1)
    private void nova$invalidateBeforePassThroughBind(final int array) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
        GL30.glBindVertexArray(array);
    }

    @Redirect(method = "glDeleteVertexArrays(I)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glDeleteVertexArrays(I)V"),
        remap = false, require = 1)
    private void nova$invalidateBeforePassThroughDelete(final int array) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
        GL30.glDeleteVertexArrays(array);
    }
}
