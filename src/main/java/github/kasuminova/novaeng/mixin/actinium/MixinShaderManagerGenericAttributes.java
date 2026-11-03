package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplies exact COLOR and SECONDARY_UV array enablement while GLSM flushes generic constants.
 */
@Mixin(value = ShaderManager.class, remap = false)
public abstract class MixinShaderManagerGenericAttributes {

    @Inject(method = "preDraw(ZZZZ)V", at = @At("HEAD"), remap = false, require = 1)
    private void nova$beginDrawFormat(final boolean hasColor,
                                      final boolean hasNormal,
                                      final boolean hasTexCoord,
                                      final boolean hasLightmap,
                                      final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().pushDrawFormat(hasColor, hasLightmap);
    }

    @Inject(method = "preDraw(ZZZZ)V", at = @At("RETURN"), remap = false, require = 1)
    private void nova$finishDrawFormat(final boolean hasColor,
                                       final boolean hasNormal,
                                       final boolean hasTexCoord,
                                       final boolean hasLightmap,
                                       final CallbackInfo ci) {
        GenericAttributeStateImpl.instance().finishDrawFormat(hasColor, hasLightmap);
    }
}
