package github.kasuminova.novaeng.common.util;

import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.util.Arrays;

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

    private static final VarHandle PARAMETERS = MethodHandles.arrayElementVarHandle(Class[][].class);

    private static final ClassValue<Table> TABLES = new ClassValue<>() {
        @Override
        protected Table computeValue(final @NonNull Class<?> type) {
            final Method[] methods = type.getDeclaredMethods();
            return new Table(methods);
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
        Method selected = null;
        for (int index = table.heads[name.hashCode() & (table.heads.length - 1)]; index >= 0; index = table.next[index]) {
            final Method method = table.methods[index];
            if (!method.getName().equals(name) || method.getParameterCount() != parameterTypes.length) continue;
            Class<?>[] parameters = (Class<?>[]) PARAMETERS.getAcquire(table.parameterTypes, index);
            if (parameters == null) {
                parameters = method.getParameterTypes();
                PARAMETERS.setRelease(table.parameterTypes, index, parameters);
            }
            if (Arrays.equals(parameters, parameterTypes)
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

    private static final class Table {
        private final Method[] methods;
        private final Class<?>[][] parameterTypes;
        private final int[] heads;
        private final int[] next;

        private Table(final Method[] methods) {
            this.methods = methods;
            this.parameterTypes = new Class<?>[methods.length][];
            int capacity = 1;
            while (capacity < methods.length) capacity <<= 1;
            this.heads = new int[capacity];
            this.next = new int[methods.length];
            Arrays.fill(heads, -1);
            // Reverse insertion preserves the VM's declaration order, including equal covariant bridges.
            for (int index = methods.length - 1; index >= 0; index--) {
                final int bucket = methods[index].getName().hashCode() & (capacity - 1);
                next[index] = heads[bucket];
                heads[bucket] = index;
            }
        }
    }

}
