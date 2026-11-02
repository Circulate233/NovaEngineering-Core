package github.kasuminova.novaeng.mixin.journeymap;

import journeymap.client.texture.TextureCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureCache.class, remap = false)
public abstract class MixinWidgetTexturePreload {
    @Inject(method = "resetSprite", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$loadWidgetTexturesOnDemand(final CallbackInfo ci) {
        // This helper only prewarms four textures; the drawing paths call getTexture themselves.
        ci.cancel();
    }
}
