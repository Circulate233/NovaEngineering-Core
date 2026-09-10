package github.kasuminova.novaeng.mixin.obscuretooltips;

import dev.obscuria.tooltips.client.render.GuiGraphics;
import dev.obscuria.tooltips.client.tooltip.element.frame.NineSlicedFrame;
import dev.obscuria.tooltips.util.color.ARGB;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NineSlicedFrame.class, remap = false)
public class MixinNineSlicedFrame {

    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Ldev/obscuria/tooltips/client/render/GuiGraphics;blit(Lnet/minecraft/util/ResourceLocation;IIFFIIII)V"))
    private void novaeng$tintedFrame(final GuiGraphics graphics, final ResourceLocation texture,
                                     final int x, final int y, final float u, final float v,
                                     final int width, final int height, final int texWidth, final int texHeight) {
        final ARGB tint = ObscureTooltipGeometry.frameTint();
        if (tint == null) {
            graphics.blit(texture, x, y, u, v, width, height, texWidth, texHeight);
            return;
        }
        graphics.blitGlow(texture, x, y, u, v, width, height, texWidth, texHeight, tint);
    }
}
