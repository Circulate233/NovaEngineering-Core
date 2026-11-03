package github.kasuminova.novaeng.mixin.biomesoplenty;

import biomesoplenty.api.generation.BOPGeneratorBase;
import github.kasuminova.novaeng.common.performance.BopStageCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BOPGeneratorBase.class, remap = false)
public abstract class MixinGeneratorStageRevision {
    @Inject(method = "setStage", at = @At("RETURN"), require = 1)
    private void nova$stageChanged(final CallbackInfo ci) {
        BopStageCache.stageChanged();
    }
}
