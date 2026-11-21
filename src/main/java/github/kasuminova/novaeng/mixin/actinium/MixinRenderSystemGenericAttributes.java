package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.GenericAttributeStateImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tracks the direct RenderSystem setter that bypasses GLSMCurrentAttribs.
 */
@Mixin(value = RenderSystem.class, remap = false)
public abstract class MixinRenderSystemGenericAttributes {

    @Redirect(method = "vertexAttrib4f(IFFFF)V",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;vertexAttrib4f(IFFFF)V"),
        remap = false, require = 1)
    private static void nova$recordDirectAttribute(final RenderBackend backend, final int index,
                                                   final float x, final float y, final float z, final float w) {
        backend.vertexAttrib4f(index, x, y, z, w);
        GenericAttributeStateImpl.instance().recordAttribute(index, x, y, z, w);
    }
}
