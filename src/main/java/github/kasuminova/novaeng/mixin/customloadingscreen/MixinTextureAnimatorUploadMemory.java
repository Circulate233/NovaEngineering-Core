package github.kasuminova.novaeng.mixin.customloadingscreen;

import github.kasuminova.novaeng.client.memory.LoadingScreenUploadBuffer;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Map;

@Pseudo
@Mixin(targets = "alexiil.mc.mod.load.render.TextureAnimator", remap = false)
public abstract class MixinTextureAnimatorUploadMemory {
    @Shadow
    private Map<?, ?> animatedTextures;

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GLAllocation;createDirectIntBuffer(I)Ljava/nio/IntBuffer;", remap = true), require = 1)
    private static IntBuffer nova$deferUploadStorage(final int originalCapacity) {
        return IntBuffer.allocate(0);
    }

    /**
     * @author NovaEngineering
     * @reason Keep the original upload order and GL parameters with bounded, reusable pixel storage.
     */
    @Overwrite
    public static void uploadTextureImage(final int textureId, final BufferedImage image) {
        TextureUtil.allocateTexture(textureId, image.getWidth(), image.getHeight());
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10242, 10497);
        GL11.glTexParameteri(3553, 10243, 10497);
        synchronized (LoadingScreenUploadBuffer.class) {
            final int width = image.getWidth();
            final int rows = LoadingScreenUploadBuffer.rowsPerChunk(width);
            for (int row = 0; row < image.getHeight(); row += rows) {
                final IntBuffer buffer = LoadingScreenUploadBuffer.copyRows(image, row);
                GL11.glTexSubImage2D(3553, 0, 0, row, width, buffer.remaining() / width, 32993, 33639, buffer);
            }
        }
    }

    @Inject(method = "close", at = @At("RETURN"), require = 1)
    private void nova$releaseUploadStorage(final CallbackInfo ci) {
        animatedTextures.clear();
        LoadingScreenUploadBuffer.release();
    }
}
