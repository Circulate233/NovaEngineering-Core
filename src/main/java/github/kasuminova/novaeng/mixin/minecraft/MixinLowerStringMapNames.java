package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.RootLowercaseCache;
import net.minecraft.util.LowerStringMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

@Mixin(LowerStringMap.class)
public abstract class MixinLowerStringMapNames {
    @Redirect(method = {"put(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", "get", "containsKey", "remove"},
        at = @At(value = "INVOKE", target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"), require = 4)
    private String nova$reuseNormalizedName(final String name, final Locale locale) {
        return RootLowercaseCache.lower(name, locale);
    }
}
