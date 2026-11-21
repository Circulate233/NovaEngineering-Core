package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.memory.TexturePixelTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

@Mixin(TextureUtil.class)
public abstract class MixinTextureUploadCapacity {
    @Unique
    private static final int nova$uploadPixels = 64 * 1024;
    @Shadow
    @Final
    @Mutable
    private static IntBuffer DATA_BUFFER;

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 4194304), require = 1)
    private static int nova$smallUploadBuffer(final int original) {
        return nova$uploadPixels;
    }

    @ModifyConstant(method = "uploadTextureSub", constant = @Constant(intValue = 4194304), require = 1)
    private static int nova$boundedArrayUpload(final int original, final int level, final int[] pixels,
                                               final int width, final int height, final int x, final int y,
                                               final boolean blur, final boolean clamp, final boolean mipmap) {
        return nova$ensureUploadCapacity(width);
    }

    @ModifyConstant(method = "uploadTextureImageSubImpl", constant = @Constant(intValue = 4194304), require = 1)
    private static int nova$boundedImageUpload(final int original, final BufferedImage image,
                                               final int x, final int y, final boolean blur, final boolean clamp) {
        return nova$ensureUploadCapacity(image.getWidth());
    }

    @Unique
    private static int nova$ensureUploadCapacity(final int width) {
        final int capacity = Math.max(nova$uploadPixels, width);
        if (DATA_BUFFER.capacity() < capacity) DATA_BUFFER = GLAllocation.createDirectIntBuffer(capacity);
        return capacity;
    }

    /**
     * @author NovaEngineering
     * @reason Avoid re-converting and allocating the complete source image for every anaglyph chunk.
     */
    @Overwrite
    private static void copyToBufferPos(final int[] pixels, final int offset, final int length) {
        TexturePixelTransfer.copy(DATA_BUFFER, pixels, offset, length, Minecraft.getMinecraft().gameSettings.anaglyph);
    }
}
