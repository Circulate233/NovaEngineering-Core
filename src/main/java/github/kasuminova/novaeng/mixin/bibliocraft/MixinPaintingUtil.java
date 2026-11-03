package github.kasuminova.novaeng.mixin.bibliocraft;

import github.kasuminova.novaeng.client.resource.BiblioCraftPaintingCache;
import jds.bibliocraft.helpers.PaintingUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuses only BiblioCraft's jar painting scan within one resource generation. */
@Mixin(value = PaintingUtil.class, remap = false)
public abstract class MixinPaintingUtil {

    @Inject(method = "getJarPaintings", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$returnCachedJarPaintings(final CallbackInfoReturnable<String[]> cir) {
        if (BiblioCraftPaintingCache.hasCachedResult()) {
            cir.setReturnValue(BiblioCraftPaintingCache.getCachedResult());
        }
    }

    @Inject(method = "getJarPaintings", at = @At("RETURN"), require = 1)
    private static void nova$cacheJarPaintings(final CallbackInfoReturnable<String[]> cir) {
        BiblioCraftPaintingCache.cacheResult(cir.getReturnValue());
    }
}
