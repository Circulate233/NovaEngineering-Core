package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.saving.SavingFieldCache;
import github.kasuminova.novaeng.common.performance.JavaSavedFieldReflection;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.util.Collection;

@Mixin(value = SavingFieldCache.class, remap = false)
public abstract class MixinSavedJavaFields {

    @Redirect(method = "buildClassFields", at = @At(value = "INVOKE",
        target = "Lkotlin/reflect/full/KClasses;getDeclaredMemberProperties(Lkotlin/reflect/KClass;)Ljava/util/Collection;"), require = 1)
    private Collection<?> nova$unhandledProperties(final KClass<?> $this$declaredMemberProperties) {
        return JavaSavedFieldReflection.declaredProperties($this$declaredMemberProperties);
    }

    @Redirect(method = {"getKotlinFieldGetter", "getKotlinFieldSetter"}, at = @At(value = "INVOKE",
        target = "Lkotlin/reflect/jvm/ReflectJvmMapping;getKotlinProperty(Ljava/lang/reflect/Field;)Lkotlin/reflect/KProperty;"), require = 1)
    private KProperty<?> nova$kotlinProperty(final Field $this$kotlinProperty) {
        return JavaSavedFieldReflection.kotlinProperty($this$kotlinProperty);
    }
}
