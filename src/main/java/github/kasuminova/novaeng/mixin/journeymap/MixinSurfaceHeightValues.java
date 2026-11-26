package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapHeightValues;
import journeymap.client.cartography.render.SurfaceRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the nullable Integer API and the original control flow, without boxing each y--. */
@Mixin(value = SurfaceRenderer.class, remap = false)
public abstract class MixinSurfaceHeightValues {
    @Redirect(method = {"getBlockHeight", "buildStrata", "paintStrata"},
        at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"),
        remap = false, require = 1)
    private Integer nova$heightValue(final int value) {
        return JourneyMapHeightValues.valueOf(value);
    }
}
