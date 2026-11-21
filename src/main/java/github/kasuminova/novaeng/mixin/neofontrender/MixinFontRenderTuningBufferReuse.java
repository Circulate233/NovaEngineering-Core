package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.FontGlStateScratch;
import github.kasuminova.novaeng.client.font.FontProjection;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

@Pseudo
@Mixin(targets = "neofontrender.core.font.support.FontRenderTuning", remap = false)
public abstract class MixinFontRenderTuningBufferReuse {
    @Shadow
    @Final
    private static float[] MODELVIEW;
    @Shadow
    @Final
    private static float[] PROJECTION;

    @Redirect(
        method = "updateFromCurrentGlState(Z)Lneofontrender/core/font/support/FontRenderTuning$DrawContext;",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/BufferUtils;createFloatBuffer(I)Ljava/nio/FloatBuffer;", remap = false),
        remap = false,
        require = 1
    )
    private static FloatBuffer nova$reuseMatrixBuffer(final int size) {
        return FontGlStateScratch.floats(size);
    }

    @Redirect(method = "updateFromCurrentGlState(Z)Lneofontrender/core/font/support/FontRenderTuning$DrawContext;",
        at = @At(value = "INVOKE", target = "Lneofontrender/core/font/support/FontRenderTuning;projectToFramebuffer([F[FFFFII)[F"), require = 1)
    private static float[] nova$projectOriginWithoutVectors(final float[] modelview, final float[] projection, final float x,
                                                            final float y, final float z, final int width, final int height) {
        return FontProjection.origin(modelview, projection, x, y, z, width, height);
    }

    /**
     * @author NovaEngineering @reason Avoid three projected points and their eye/clip arrays per draw.
     */
    @Overwrite
    static float measureProjectedPixelScale(final float[] modelview, final float[] projection, final int width, final int height) {
        return FontProjection.pixelScale(modelview, projection, width, height);
    }

    /**
     * @author NovaEngineering @reason Evaluate projected corner bounds with scalars.
     */
    @Overwrite
    public static boolean isCurrentTextQuadVisible(final float x, final float y, final float width, final float height, final float minPixelHeight) {
        final Minecraft mc = Minecraft.getMinecraft();
        return mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0
            || FontProjection.visible(MODELVIEW, PROJECTION, x, y, width, height, minPixelHeight, mc.displayWidth, mc.displayHeight);
    }
}
