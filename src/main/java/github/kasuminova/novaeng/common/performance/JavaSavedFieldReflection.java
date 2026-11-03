package github.kasuminova.novaeng.common.performance;

import com.teamwizardry.librarianlib.features.saving.Save;
import kotlin.Metadata;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;

/**
 * Skips Kotlin property discovery only where LibrarianLib already saves every Java field.
 */
public final class JavaSavedFieldReflection {

    private static final ClassValue<Boolean> SAVED_JAVA_FIELDS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(@NotNull final Class<?> type) {
            if (type == Object.class) {
                return true;
            }
            if (type.isInterface() || type.isEnum() || type.isRecord() || type.isSynthetic()
                || type.isAnnotationPresent(Metadata.class)) {
                return false;
            }
            // Java implementations of Kotlin property accessors can expose declared
            // properties without a backing field. Keep accessor-bearing classes on
            // Kotlin's normal discovery path, including JavaBean-style getters.
            for (final Method method : type.getDeclaredMethods()) {
                final String name = method.getName();
                if (!Modifier.isStatic(method.getModifiers())
                    && (name.startsWith("get") || name.startsWith("is") || name.startsWith("set"))) {
                    return false;
                }
            }
            for (final Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(Save.class)) {
                    return false;
                }
            }
            return true;
        }
    };

    private JavaSavedFieldReflection() {
    }

    public static Collection<?> declaredProperties(final KClass<?> type) {
        // Java declared properties represent fields. Every eligible instance field
        // enters buildClassFields' appliedFields set before its property loop.
        return SAVED_JAVA_FIELDS.get(JvmClassMappingKt.getJavaClass(type))
            ? Collections.emptyList() : KClasses.getDeclaredMemberProperties(type);
    }

    public static KProperty<?> kotlinProperty(final Field field) {
        // For these ordinary Java fields Kotlin's getter/setter is a field caller,
        // with no Java Method. LibrarianLib therefore uses its Java field handle.
        return SAVED_JAVA_FIELDS.get(field.getDeclaringClass())
            ? null : ReflectJvmMapping.getKotlinProperty(field);
    }
}
