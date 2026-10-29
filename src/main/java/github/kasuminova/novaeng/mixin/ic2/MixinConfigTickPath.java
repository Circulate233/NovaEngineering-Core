package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.performance.IC2ConfigPaths;
import ic2.core.util.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = Config.class, remap = false)
public abstract class MixinConfigTickPath {

    @Shadow
    private static List<String> split(final String str, final char splitChar) {
        throw new AssertionError();
    }

    @Redirect(method = "get(Ljava/lang/String;)Lic2/core/util/Config$Value;", at = @At(value = "INVOKE",
        target = "Lic2/core/util/Config;split(Ljava/lang/String;C)Ljava/util/List;"), require = 1)
    private static List<String> nova$splitTickPath(final String str, final char splitChar) {
        final List<String> path = IC2ConfigPaths.tickPath(str, splitChar);
        return path != null ? path : split(str, splitChar);
    }
}
