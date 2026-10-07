package github.kasuminova.novaeng.mixin.journeymap;

import journeymap.client.ui.UIManager;
import mezz.jei.Internal;
import mezz.jei.runtime.JeiRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides JourneyMap's minimap while JEI's item list is displayed over the current screen. */
@Mixin(value = UIManager.class, remap = false)
public abstract class MixinUIManagerJeiOverlay {

    @Inject(method = "drawMiniMap", at = @At("HEAD"), cancellable = true, remap = false)
    private void nova$hideMinimapBehindJeiOverlay(final CallbackInfo ci) {
        final JeiRuntime runtime = Internal.getRuntime();
        if (runtime != null && runtime.getIngredientListOverlay().isListDisplayed()) {
            ci.cancel();
        }
    }
}
