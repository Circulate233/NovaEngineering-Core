package github.kasuminova.novaeng.mixin.zenscript;

import github.kasuminova.novaeng.common.util.MethodMetadataCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import stanhebben.zenscript.type.natives.JavaMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Answers the reflection done while wrapping a native method from cached arrays.
 *
 * <p>The wrapper asks for the method's generic parameter types and parameter annotations once per parameter, and
 * each of those calls builds a fresh array. Both are properties of a loaded method, so they are read once.</p>
 */
@Mixin(value = JavaMethod.class, remap = false)
public class MixinJavaMethod {

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Ljava/lang/reflect/Method;getGenericParameterTypes()[Ljava/lang/reflect/Type;",
        remap = false), remap = false, require = 1)
    private Type[] nova$cachedGenericParameterTypes(final Method method) {
        return MethodMetadataCache.genericParameterTypes(method);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Ljava/lang/reflect/Method;getParameterAnnotations()[[Ljava/lang/annotation/Annotation;",
        remap = false), remap = false, require = 1)
    private Annotation[][] nova$cachedParameterAnnotations(final Method method) {
        return MethodMetadataCache.parameterAnnotations(method);
    }

}
