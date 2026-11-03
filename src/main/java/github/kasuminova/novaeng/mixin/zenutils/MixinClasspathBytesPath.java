package github.kasuminova.novaeng.mixin.zenutils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import youyihj.zenutils.impl.member.bytecode.ClasspathBytesProvider;

@Mixin(value = ClasspathBytesProvider.class, remap = false)
public abstract class MixinClasspathBytesPath {

    @Redirect(method = "getClassBytes", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"), require = 1)
    private String[] nova$joinClassPathOnce(final String className, final String regex) {
        if (!"\\.".equals(regex) || className.startsWith(".") || className.endsWith(".")
            || className.contains("..")) {
            return className.split(regex);
        }
        // The original method appends .class, then passes first/more to every ZIP filesystem.
        // An already joined first component avoids building the same path again for each JAR.
        return new String[]{className.replace('.', '/')};
    }
}
