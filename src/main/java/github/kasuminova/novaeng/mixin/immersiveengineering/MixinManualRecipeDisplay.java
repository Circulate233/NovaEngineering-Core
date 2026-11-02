package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.lib.manual.ManualPages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pack omits crafting diagrams from IE manuals; their description text remains.
 */
@Mixin(value = {ManualPages.Crafting.class, ManualPages.CraftingMulti.class}, remap = false)
public abstract class MixinManualRecipeDisplay {
    @Inject(method = "recalculateCraftingRecipes", at = @At("HEAD"), remap = false,
        cancellable = true, require = 1)
    private void nova$omitRecipeDisplay(final CallbackInfo ci) {
        ci.cancel();
    }
}
