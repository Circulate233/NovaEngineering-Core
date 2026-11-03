package github.kasuminova.novaeng.mixin.journeymap;

import journeymap.client.texture.TextureCache;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;
import java.util.Set;

@Mixin(value = TextureCache.class, remap = false)
public abstract class MixinTextureCacheArchiveNames {

    @Redirect(method = "addArchiveEntry", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"), require = 1)
    private static String nova$pngSuffix(final String name, final Locale locale) {
        if (locale != Locale.ROOT) {
            return name.toLowerCase(locale);
        }
        // This value is consumed only by the immediately following endsWith(".png").
        // The original name remains available for the case-preserving resource path.
        return name.regionMatches(true, name.length() - 4, ".png", 0, 4) ? ".png" : "";
    }

    @Redirect(method = "addArchiveEntry", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;indexOf(II)I"), require = 1)
    private static int nova$matchingDomainEnd(final String archiveName, final int ch, final int fromIndex,
                                              final String name, final String prefix, final Set<ResourceLocation> out) {
        final int domainEnd = archiveName.indexOf(ch, fromIndex);
        // Reject unrelated texture directories before the original code allocates
        // its namespace and resource-path substrings.
        return domainEnd >= 0 && archiveName.startsWith(prefix, domainEnd + 1) ? domainEnd : -1;
    }
}
