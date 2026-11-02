package github.kasuminova.novaeng.mixin.minecraft;

import com.gtnewhorizons.angelica.glsm.GLStateManager;

import github.kasuminova.novaeng.client.gl.TextureUploadBindingScope;

import net.minecraft.client.renderer.texture.TextureUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.image.BufferedImage;

/**
 * Capture the known binding after TextureUtil's bind, only for the callback-free upload body.
 */
@Mixin(TextureUtil.class)
public abstract class MixinDynamicTextureUploadBinding {
    @Shadow
    private static void uploadTextureSub(
        final int level,
        final int[] pixels,
        final int width,
        final int height,
        final int x,
        final int y,
        final boolean blur,
        final boolean clamp,
        final boolean mipmap) {
        throw new AssertionError();
    }

    @Shadow
    private static void uploadTextureImageSubImpl(
        final BufferedImage image,
        final int x,
        final int y,
        final boolean blur,
        final boolean clamp) {
        throw new AssertionError();
    }

    @Redirect(
        method = "uploadTexture",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/client/renderer/texture/TextureUtil;uploadTextureSub(I[IIIIIZZZ)V"),
        require = 1)
    private static void nova$uploadPixels(
        final int level,
        final int[] pixels,
        final int width,
        final int height,
        final int x,
        final int y,
        final boolean blur,
        final boolean clamp,
        final boolean mipmap,
        final int textureId,
        final int[] sourcePixels,
        final int sourceWidth,
        final int sourceHeight) {
        if (GLStateManager.isCachingEnabled() || GLStateManager.BYPASS_CACHE) {
            uploadTextureSub(level, pixels, width, height, x, y, blur, clamp, mipmap);
            return;
        }
        final int previous = TextureUploadBindingScope.begin(textureId);
        try {
            uploadTextureSub(level, pixels, width, height, x, y, blur, clamp, mipmap);
        } finally {
            TextureUploadBindingScope.end(previous);
        }
    }

    @Redirect(
        method = "uploadTextureImageSub",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/client/renderer/texture/TextureUtil;uploadTextureImageSubImpl(Ljava/awt/image/BufferedImage;IIZZ)V"),
        require = 1)
    private static void nova$uploadImage(
        final BufferedImage image,
        final int x,
        final int y,
        final boolean blur,
        final boolean clamp,
        final int textureId,
        final BufferedImage sourceImage,
        final int sourceX,
        final int sourceY,
        final boolean sourceBlur,
        final boolean sourceClamp) {
        if (GLStateManager.isCachingEnabled() || GLStateManager.BYPASS_CACHE) {
            uploadTextureImageSubImpl(image, x, y, blur, clamp);
            return;
        }
        final int previous = TextureUploadBindingScope.begin(textureId);
        try {
            uploadTextureImageSubImpl(image, x, y, blur, clamp);
        } finally {
            TextureUploadBindingScope.end(previous);
        }
    }
}
