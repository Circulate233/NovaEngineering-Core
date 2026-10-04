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
 *
 * <p>Two targets because the embeddium package moved between Actinium releases: older builds ship it under
 * {@code org.embeddedt.embeddium}, the current one under {@code dhj.embeddedt.embeddium}. Only the copy that
 * resolves is applied, so both are declared pseudo with optional injection.</p>
 */
@Pseudo
@Mixin(targets = "dhj.embeddedt.embeddium.impl.gl.device.GLRenderDevice$ImmediateCommandList", remap = false)
public abstract class MixinImmediateCommandListVertexArray {

    @Inject(method = "bindVertexArray", at = @At("HEAD"), remap = false, require = 0)
    private void nova$invalidateBeforeRawBind(@Coerce final Object array, final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }

    @Inject(method = "unbindVertexArray", at = @At("HEAD"), remap = false, require = 0)
    private void nova$invalidateBeforeRawUnbind(final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().invalidateVertexArray();
    }
}
