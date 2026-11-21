package github.kasuminova.novaeng.mixin.zenscript;

import github.kasuminova.novaeng.common.performance.ScriptPathSegments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import stanhebben.zenscript.ZenModule;

import java.io.File;

@Mixin(value = ZenModule.class, remap = false)
public abstract class MixinScriptPathParsing {
    @Redirect(method = "compileScripts", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), require = 1)
    private static String nova$removeZipSuffix(final String value, final String regex, final String replacement) {
        return "\\.zip".equals(regex) && replacement.isEmpty() ? value.replace(".zip", "") : value.replaceAll(regex, replacement);
    }

    @Redirect(method = "compileScripts", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"), require = 1)
    private static String[] nova$splitScriptPath(final String value, final String regex) {
        return ("\\.|\\" + File.separator).equals(regex)
            ? ScriptPathSegments.split(value, File.separatorChar) : value.split(regex);
    }
}
