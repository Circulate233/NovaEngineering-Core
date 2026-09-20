package github.kasuminova.novaeng.mixin.bibliocraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.resource.BiblioCraftPaintingCache;
import jds.bibliocraft.helpers.PaintingUtil;
import org.spongepowered.asm.mixin.Mixin;

/** Reuses only BiblioCraft's jar painting scan within one resource generation. */
@Mixin(value = PaintingUtil.class, remap = false)
public abstract class MixinPaintingUtil {

    @WrapMethod(method = "getJarPaintings", require = 1)
    private static String[] nova$cacheJarPaintings(final Operation<String[]> original) {
        return BiblioCraftPaintingCache.getOrCompute(original::call);
    }
}
