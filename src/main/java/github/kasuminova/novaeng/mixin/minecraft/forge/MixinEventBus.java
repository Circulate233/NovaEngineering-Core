package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.util.DeclaredMethodTable;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;

/**
 * Removes the exceptions Forge's event registration walks through on every launch.
 *
 * <p>A class is registered once, so the cost is not the number of registrations: it is inside a single
 * {@code register} call, which asks a reflective question per public method per supertype, and every miss costs a
 * thrown and caught {@code NoSuchMethodException}, i.e. a filled-in stack trace. Both questions live in different
 * methods — the probe for a {@code forceClassLoadingForDeclaredMethods} method that never exists is in
 * {@code register} itself, and the supertype lookup is the body of {@code lambda$register$0} — so both are
 * redirected.</p>
 *
 * <p>Both are answered by {@link DeclaredMethodTable}, which builds one table per class with
 * {@link Class#getDeclaredMethods()}: the JVM caches that call, it never throws, and reading it resolves every
 * declared method's parameter types, which is the class loading the caller's probe relied on. Linkage errors the
 * callers expect to fall through still propagate.</p>
 */
@Mixin(value = EventBus.class, remap = false)
public class MixinEventBus {

    @Redirect(method = "register(Ljava/lang/Object;)V", at = @At(value = "INVOKE",
        target = "Ljava/lang/Class;getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
        remap = false), remap = false, require = 1)
    private Method nova$probeFromCache(final Class<?> owner,
                                       final String name,
                                       final Class<?>[] parameterTypes) {
        return DeclaredMethodTable.lookup(owner, name, parameterTypes);
    }

    /**
     * Covers the supertype lookup, which the compiler moved into a synthetic method of its own.
     *
     * <p>That method is static, so this handler is too; the receiver of the redirected call is still passed as the
     * first argument. The name is compiler generated and therefore tied to the Forge build being run, so a mismatch
     * fails loudly here rather than silently leaving the exceptions in place.</p>
     */
    @Redirect(method = "lambda$register$0", at = @At(value = "INVOKE",
        target = "Ljava/lang/Class;getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
        remap = false), remap = false, require = 1)
    private static Method nova$superLookupFromCache(final Class<?> owner,
                                                    final String name,
                                                    final Class<?>[] parameterTypes) {
        return DeclaredMethodTable.lookup(owner, name, parameterTypes);
    }

}
