package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.memory.DynamicFontCaches;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CosmicTextRenderer.class, remap = false)
public abstract class MixinCosmicCacheLifecycle {
    @Shadow
    @Final
    private boolean benchmarkMode;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/resources/IResourceManager;Lneofontrender/api/text/FontRenderSpec;Z)V",
        at = @At("RETURN"), require = 1)
    private void nova$registerDynamicCache(final CallbackInfo ci) {
        if (!benchmarkMode) DynamicFontCaches.register((CosmicTextRenderer) (Object) this);
    }

    @Inject(method = "close", at = @At("RETURN"), require = 1)
    private void nova$unregisterDynamicCache(final CallbackInfo ci) {
        if (!benchmarkMode) DynamicFontCaches.unregister((CosmicTextRenderer) (Object) this);
    }
}
