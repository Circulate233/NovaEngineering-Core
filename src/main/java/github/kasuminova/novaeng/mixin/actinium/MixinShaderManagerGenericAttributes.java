package github.kasuminova.novaeng.mixin.actinium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.gl.GenericAttributeState;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import com.gtnewhorizons.angelica.glsm.ffp.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Supplies exact COLOR and SECONDARY_UV array enablement while GLSM flushes generic constants.
 */
@Mixin(value = ShaderManager.class, remap = false)
public abstract class MixinShaderManagerGenericAttributes {

    @WrapMethod(method = "preDraw(ZZZZ)V", remap = false, require = 1)
    private void nova$withDrawFormat(final boolean hasColor,
                                     final boolean hasNormal,
                                     final boolean hasTexCoord,
                                     final boolean hasLightmap,
                                     final Operation<Void> original) {
        final GenericAttributeState state = GenericAttributeStateImpl.instance();
        state.pushDrawFormat(hasColor, hasLightmap);
        try {
            original.call(hasColor, hasNormal, hasTexCoord, hasLightmap);
        } finally {
            state.finishDrawFormat(hasColor, hasLightmap);
        }
    }
}
