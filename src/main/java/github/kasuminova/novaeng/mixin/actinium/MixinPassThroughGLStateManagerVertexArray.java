package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import com.mitchej123.glsm.impl.PassThroughGLStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates the VAO shadow before the pass-through GL service issues a raw LWJGL bind.
 */
@Mixin(value = PassThroughGLStateManager.class, remap = false)
public abstract class MixinPassThroughGLStateManagerVertexArray {

    @Inject(method = "glBindVertexArray(I)V", at = @At("HEAD"),
        remap = false, require = 1)
    private void nova$invalidateBeforePassThroughBind(final int array, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }
}
