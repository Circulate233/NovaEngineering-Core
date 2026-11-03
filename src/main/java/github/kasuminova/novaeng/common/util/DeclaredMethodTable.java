package github.kasuminova.novaeng.common.util;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Answers declared-method lookups from a per-class table instead of the throwing reflective call.
 *
 * <p>Forge's event registration asks such questions once per method per supertype, and every miss costs a thrown
 * and caught {@code NoSuchMethodException}, i.e. a filled-in stack trace. A table built with
 * {@link Class#getDeclaredMethods()} answers the same question, is cached by the JVM, and never throws.</p>
 *
 * <p>Covariant bridge methods can share a name and parameter list; lookup selects the most specific return type,
 * as {@link Class#getDeclaredMethod(String, Class[])} does. The only exception callers handle is
 * {@code NoSuchMethodException}, represented by a null result. Reading the table also resolves parameter types, which is
 * the class loading the caller's own probe for a non-existent method relied on.</p>
 *
 * <p>Keyed by class and read from whatever thread is registering: registration is a public API, so this stays a
 * concurrent table rather than inheriting any single-threaded assumption.</p>
 */
public final class DeclaredMethodTable {

    private static final Comparator<Method> BY_NAME = Comparator.comparing(Method::getName);

    private static final ClassValue<Table> TABLES = new ClassValue<>() {
        @Override
        protected Table computeValue(final @NonNull Class<?> type) {
            final Method[] methods = type.getDeclaredMethods();
            // Stable sorting preserves the VM's tie order for covariant bridge methods.
            Arrays.sort(methods, BY_NAME);
            final Class<?>[][] parameterTypes = new Class<?>[methods.length][];
            for (int index = 0; index < methods.length; index++) {
                parameterTypes[index] = methods[index].getParameterTypes();
            }
            return new Table(methods, parameterTypes);
        }
    };

    private static final ClassValue<Method[]> PUBLIC_TABLES = new ClassValue<>() {
        @Override
        protected Method[] computeValue(final @NonNull Class<?> type) {
            return type.getMethods();
        }
    };

    private DeclaredMethodTable() {
    }

    /**
     * Looks up a method a class declares itself, with the exact parameter types asked for.
     *
     * @param owner class to search
     * @param name method name
     * @param parameterTypes exact parameter types
     * @return the matching declared method, or {@code null} when the class does not declare one
     */
    public static Method lookup(final Class<?> owner, final String name, final Class<?>[] parameterTypes) {
        final Table table = TABLES.get(owner);
        final Method[] methods = table.methods;
        int low = 0;
        int high = methods.length;
        while (low < high) {
            final int middle = (low + high) >>> 1;
            if (methods[middle].getName().compareTo(name) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        Method selected = null;
        for (int index = low; index < methods.length; index++) {
            final Method method = methods[index];
            if (!method.getName().equals(name)) {
                break;
            }
            if (table.parameterTypes[index].length == parameterTypes.length
                && Arrays.equals(table.parameterTypes[index], parameterTypes)
                && (selected == null || selected.getReturnType().isAssignableFrom(method.getReturnType()))) {
                selected = method;
            }
        }
        return selected;
    }

    /**
     * Returns the immutable-for-registration public method array for one class.
     */
    public static Method[] publicMethods(final Class<?> owner) {
        return PUBLIC_TABLES.get(owner);
    }

    private record Table(Method[] methods, Class<?>[][] parameterTypes) {
    }

}
