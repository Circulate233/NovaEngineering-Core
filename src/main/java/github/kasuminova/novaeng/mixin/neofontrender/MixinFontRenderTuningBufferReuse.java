package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.FontGlStateScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

@Mixin(targets = "neofontrender.core.font.support.FontRenderTuning", remap = false)
public abstract class MixinFontRenderTuningBufferReuse {
    @Redirect(
        method = "updateFromCurrentGlState(Z)Lneofontrender/core/font/support/FontRenderTuning$DrawContext;",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/BufferUtils;createFloatBuffer(I)Ljava/nio/FloatBuffer;", remap = false),
        remap = false,
        require = 1
    )
    private static FloatBuffer nova$reuseMatrixBuffer(final int size) {
        return FontGlStateScratch.floats(size);
    }
}
