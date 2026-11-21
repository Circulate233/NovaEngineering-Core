package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.HeiRecipeIndexCompaction;
import github.kasuminova.novaeng.common.util.collection.WeakCacheMaintenance;
import mezz.jei.Internal;
import mezz.jei.startup.JeiStarter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JeiStarter.class, remap = false)
public abstract class MixinJeiStarterHashCacheCleanup {
    @Inject(method = "load", at = @At("RETURN"))
    private void nova$finishIngredientHashCache(final CallbackInfo ci) {
        WeakCacheMaintenance.loadingComplete();
        if (Internal.getRuntime().getRecipeRegistry() instanceof HeiRecipeIndexCompaction indexes) {
            indexes.nova$compactRecipeIndexes();
        }
    }
}
