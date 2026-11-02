package github.kasuminova.novaeng.mixin.minecraft;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import github.kasuminova.novaeng.client.gl.TextureUploadBindingScope;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The original mip upload contains no binds or progress callbacks; each caller retains its lifecycle.
 */
@Mixin(TextureMap.class)
public abstract class MixinTextureMapUploadBinding extends AbstractTexture {

    @Redirect(method = "finishLoading",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureUtil;uploadTextureMipmap([[IIIIIZZ)V",
            remap = true),
        remap = false, require = 1)
    private void nova$withUploadBinding(final int[][] pixels, final int width, final int height,
                                        final int x, final int y, final boolean blur, final boolean clamp) {
        if (GLStateManager.isCachingEnabled() || GLStateManager.BYPASS_CACHE) {
            TextureUtil.uploadTextureMipmap(pixels, width, height, x, y, blur, clamp);
            return;
        }
        final int previous = TextureUploadBindingScope.begin(this.getGlTextureId());
        try {
            TextureUtil.uploadTextureMipmap(pixels, width, height, x, y, blur, clamp);
        } finally {
            TextureUploadBindingScope.end(previous);
        }
    }
}
