package github.kasuminova.novaeng.mixin.astralsorcery;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import hellfirepvp.astralsorcery.client.util.TexturePreloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TexturePreloader.class, remap = false)
public class MixinTexturePreloader {

    @Inject(method = "doPreloadRoutine", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$skipStartupPreload(final CallbackInfo ci) {
        if (NovaEngCoreConfig.CLIENT.optimizeAstralSorceryTexturePreload) {
            ci.cancel();
        }
    }
}
