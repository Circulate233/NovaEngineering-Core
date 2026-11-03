package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.FontGlStateScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Mixin(targets = "neofontrender.core.font.cosmic.CosmicTextRenderer$PremultipliedBlendState", remap = false)
public abstract class MixinCosmicBlendStateBufferReuse {
    @Redirect(
        method = "readColorMask()[Z",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/BufferUtils;createByteBuffer(I)Ljava/nio/ByteBuffer;", remap = false),
        remap = false,
        require = 1
    )
    private static ByteBuffer nova$reuseColorMaskBuffer(final int size) {
        return FontGlStateScratch.bytes(size);
    }

    @Redirect(
        method = "readColor()[F",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/BufferUtils;createFloatBuffer(I)Ljava/nio/FloatBuffer;", remap = false),
        remap = false,
        require = 1
    )
    private static FloatBuffer nova$reuseColorBuffer(final int size) {
        return FontGlStateScratch.floats(size);
    }
}
