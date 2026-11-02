package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.init.WorldHandler;
import github.kasuminova.novaeng.common.performance.PrimerOreGeneration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorldHandler.class, remap = false)
public abstract class MixinPrimerOreReload {
    @Inject(method = {"reloadConfig(Z)Z", "registerFeature(Lcofh/cofhworld/world/IFeatureGenerator;)Z",
        "removeFeature(Lcofh/cofhworld/world/IFeatureGenerator;)Z"}, at = @At("RETURN"))
    private static void nova$invalidatePrimerRules(final CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) PrimerOreGeneration.invalidate();
    }
}
