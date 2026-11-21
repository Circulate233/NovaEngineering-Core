package github.kasuminova.novaeng.mixin.vintagefix;

import org.spongepowered.asm.mixin.Mixin;
import org.embeddedt.vintagefix.dynamicresources.helpers.DefaultPackAdapter;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = DefaultPackAdapter.class, remap = false)
public abstract class MixinDefaultPackJarUri {
    @ModifyArg(method = "walkFileSystems", at = @At(value = "INVOKE",
        target = "Ljava/net/URI;<init>(Ljava/lang/String;)V"), index = 0, require = 1)
    private static String nova$completeJarRoot(final String uri) {
        return uri.startsWith("jar:") && !uri.contains("!/") ? uri + "!/" : uri;
    }
}
