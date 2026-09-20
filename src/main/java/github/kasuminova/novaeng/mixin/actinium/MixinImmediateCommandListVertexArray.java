package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates Nova's VAO shadow before Embeddium issues binds outside GLSM.
 */
@Pseudo
@Mixin(targets = "org.embeddedt.embeddium.impl.gl.device.GLRenderDevice$ImmediateCommandList", remap = false)
public abstract class MixinImmediateCommandListVertexArray {

    @Inject(method = "bindVertexArray", at = @At("HEAD"), remap = false, require = 1)
    private void nova$invalidateBeforeRawBind(@Coerce final Object array, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "unbindVertexArray", at = @At("HEAD"), remap = false, require = 1)
    private void nova$invalidateBeforeRawUnbind(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }
}
