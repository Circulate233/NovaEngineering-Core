package github.kasuminova.novaeng.mixin.biomesoplenty;

import biomesoplenty.common.remote.TrailManager;
import biomesoplenty.core.BiomesOPlenty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;


@Mixin(BiomesOPlenty.class)
public class MixinBiomesOPlenty {

    /**
     * 主线程执行网络操作，罪大恶极！
     */
    @Redirect(method = "preInit",
            at = @At(
                    value = "INVOKE",
                    target = "Lbiomesoplenty/common/remote/TrailManager;retrieveTrails()V",
                    remap = false),
            remap = false)
    @SuppressWarnings("MethodMayBeStatic")
    private void onPreInit() {
        CompletableFuture.runAsync(TrailManager::retrieveTrails);
    }

}
