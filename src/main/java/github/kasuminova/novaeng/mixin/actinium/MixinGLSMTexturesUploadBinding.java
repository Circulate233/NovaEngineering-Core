package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizons.angelica.glsm.GLSMTextures;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import github.kasuminova.novaeng.client.gl.TextureUploadBindingScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reuses a known atlas upload binding at GLSM's shared texture query implementation.
 */
@Mixin(value = GLSMTextures.class, remap = false)
public abstract class MixinGLSMTexturesUploadBinding {

    @Redirect(method = "getBoundTextureForServerState()I",
        at = @At(value = "INVOKE",
            target = "Lcom/gtnewhorizons/angelica/glsm/backend/RenderBackend;getInteger(I)I"),
        remap = false, require = 1)
    private static int nova$reuseUploadBinding(final RenderBackend backend, final int parameter) {
        final int texture = TextureUploadBindingScope.current();
        return texture >= 0 && parameter == 32873 ? texture : backend.getInteger(parameter);
    }
}
