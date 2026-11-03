package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.TextureUploadBindingScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps physical queries outside an atlas upload and GLSM's normal cached branch unchanged.
 */
@Mixin(value = GLStateManager.class, remap = false)
public abstract class MixinGLStateManagerUploadBinding {

    @Redirect(method = "getBoundTextureForServerState()I",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;getInteger(I)I"),
        require = 1)
    private static int nova$reuseUploadBinding(final RenderBackend backend, final int parameter) {
        final int texture = TextureUploadBindingScope.current();
        return texture >= 0 && parameter == 32873 ? texture : backend.getInteger(parameter);
    }
}
