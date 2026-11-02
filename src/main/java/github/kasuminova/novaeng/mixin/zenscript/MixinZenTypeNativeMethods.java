package github.kasuminova.novaeng.mixin.zenscript;

import github.kasuminova.novaeng.common.util.DeclaredMethodTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import stanhebben.zenscript.type.ZenTypeNative;

import java.lang.reflect.Method;

/**
 * Reuses the immutable public method array while ZenScript completes nested native types.
 */
@Mixin(value = ZenTypeNative.class, remap = false)
public abstract class MixinZenTypeNativeMethods {

    @Redirect(method = "complete",
        at = @At(value = "INVOKE", target = "Ljava/lang/Class;getMethods()[Ljava/lang/reflect/Method;",
            remap = false), remap = false, require = 1)
    private Method[] nova$publicMethodsFromCache(final Class<?> owner) {
        return DeclaredMethodTable.publicMethods(owner);
    }
}
