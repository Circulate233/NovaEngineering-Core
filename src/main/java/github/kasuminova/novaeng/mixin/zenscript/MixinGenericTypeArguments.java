package github.kasuminova.novaeng.mixin.zenscript;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import stanhebben.zenscript.compiler.TypeRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Mixin(value = TypeRegistry.class, priority = 1100, remap = false)
public abstract class MixinGenericTypeArguments {
    @Redirect(method = {"getMapType", "getMapEntryType"}, at = @At(value = "INVOKE",
        target = "Ljava/lang/reflect/ParameterizedType;getActualTypeArguments()[Ljava/lang/reflect/Type;"), require = 4)
    private Type[] nova$readArgumentsOnce(final ParameterizedType type,
                                         @Share("nova$typeArguments") final LocalRef<Type[]> arguments) {
        Type[] result = arguments.get();
        if (result == null) {
            result = type.getActualTypeArguments();
            arguments.set(result);
        }
        return result;
    }
}
