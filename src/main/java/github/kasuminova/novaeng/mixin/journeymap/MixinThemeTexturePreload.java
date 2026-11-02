package github.kasuminova.novaeng.mixin.journeymap;

import journeymap.client.io.ThemeLoader;
import journeymap.common.Journeymap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ThemeLoader.class, remap = false)
public abstract class MixinThemeTexturePreload {
    @Redirect(method = "initialize", at = @At(value = "INVOKE",
        target = "Ljourneymap/client/io/ThemeLoader;preloadCurrentTheme()V"), require = 1)
    private static void nova$initializeThemeWithoutPreloadingTextures() {
        // Keep selection/default normalization. TextureCache already loads and caches each requested icon.
        ThemeLoader.getCurrentTheme();
        Journeymap.getLogger().debug("Theme textures will be loaded when requested");
    }
}
