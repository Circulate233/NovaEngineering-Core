package github.kasuminova.novaeng.mixin.obscuretooltips;

import dev.obscuria.tooltips.client.TooltipState;
import dev.obscuria.tooltips.client.render.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TooltipState.class, remap = false)
public abstract class MixinTooltipState {

    @Inject(method = "renderPanel", at = @At("HEAD"))
    private void novaeng$capturePanel(final GuiGraphics graphics, final int x, final int y,
                                     final int width, final int height, final CallbackInfo ci) {
        final TooltipState self = (TooltipState) (Object) this;
        ObscureTooltipGeometry.capturePanel(self.style, x, y, width, height);
        ObscureTooltipGeometry.resolveTint(self.stack);
        ObscureTooltipGeometry.beginTintScope();
    }

    @Inject(method = "renderPanel", at = @At("RETURN"))
    private void novaeng$endPanelTint(final GuiGraphics graphics, final int x, final int y,
                                      final int width, final int height, final CallbackInfo ci) {
        ObscureTooltipGeometry.endTintScope();
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void novaeng$beginFrameTint(final GuiGraphics graphics, final int x, final int y,
                                        final int width, final int height, final CallbackInfo ci) {
        ObscureTooltipGeometry.beginTintScope();
    }

    @Inject(method = "renderFrame", at = @At("RETURN"))
    private void novaeng$endFrameTint(final GuiGraphics graphics, final int x, final int y,
                                      final int width, final int height, final CallbackInfo ci) {
        ObscureTooltipGeometry.endTintScope();
    }
}
