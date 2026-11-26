package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapHeightValues;
import journeymap.client.cartography.render.TopoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TopoRenderer.class, remap = false)
public abstract class MixinTopoHeightValues {
    @Redirect(method = {"getBlockHeight", "populateSlopes"},
        at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"),
        remap = false, require = 1)
    private Integer nova$heightValue(final int value) {
        return JourneyMapHeightValues.valueOf(value);
    }
}
