package github.kasuminova.novaeng.mixin.obscuretooltips;

import dev.obscuria.tooltips.client.tooltip.particle.GraphicUtils;
import dev.obscuria.tooltips.util.color.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = GraphicUtils.class, remap = false)
public class MixinGraphicUtils {

    @ModifyVariable(method = "drawRect(Ldev/obscuria/tooltips/client/render/GuiGraphics;IIIILdev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;)V",
        at = @At("HEAD"), argsOnly = true, index = 5)
    private static ARGB novaeng$tintTopLeft(final ARGB value) {
        return ObscureTooltipGeometry.tint(value);
    }

    @ModifyVariable(method = "drawRect(Ldev/obscuria/tooltips/client/render/GuiGraphics;IIIILdev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;)V",
        at = @At("HEAD"), argsOnly = true, index = 6)
    private static ARGB novaeng$tintTopRight(final ARGB value) {
        return ObscureTooltipGeometry.tint(value);
    }

    @ModifyVariable(method = "drawRect(Ldev/obscuria/tooltips/client/render/GuiGraphics;IIIILdev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;)V",
        at = @At("HEAD"), argsOnly = true, index = 7)
    private static ARGB novaeng$tintBottomLeft(final ARGB value) {
        return ObscureTooltipGeometry.tint(value);
    }

    @ModifyVariable(method = "drawRect(Ldev/obscuria/tooltips/client/render/GuiGraphics;IIIILdev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;Ldev/obscuria/tooltips/util/color/ARGB;)V",
        at = @At("HEAD"), argsOnly = true, index = 8)
    private static ARGB novaeng$tintBottomRight(final ARGB value) {
        return ObscureTooltipGeometry.tint(value);
    }
}
