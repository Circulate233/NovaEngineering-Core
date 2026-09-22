package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers declared-method lookups from a per-class table instead of the throwing reflective call.
 *
 * <p>Forge's event registration asks such questions once per method per supertype, and every miss costs a thrown
 * and caught {@code NoSuchMethodException}, i.e. a filled-in stack trace. A table built with
 * {@link Class#getDeclaredMethods()} answers the same question, is cached by the JVM, and never throws.</p>
 *
 * <p>The answers are equivalent to the reflective ones: a class cannot declare two methods with the same name and
 * descriptor, and the only exception the callers handle is {@code NoSuchMethodException} — exactly the "no such
 * method" answer returned here. Reading the table also resolves every declared method's parameter types, which is
 * the class loading the caller's own probe for a non-existent method relied on.</p>
 *
 * <p>Keyed by class and read from whatever thread is registering: registration is a public API, so this stays a
 * concurrent table rather than inheriting any single-threaded assumption.</p>
 */
public final class DeclaredMethodTable {

    private static final Map<Class<?>, Map<String, Candidate[]>> TABLES = new ConcurrentHashMap<>();

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
        final Candidate[] candidates = TABLES.computeIfAbsent(owner, DeclaredMethodTable::build).get(name);
        if (candidates == null) {
            return null;
        }
        for (final Candidate candidate : candidates) {
            if (Arrays.equals(candidate.parameterTypes(), parameterTypes)) {
                return candidate.method();
            }
        }
        return null;
    }

    /**
     * Groups one class's declared methods by name, resolving their parameter types on the way.
     *
     * @param owner class to read
     * @return name to candidates table, not modified afterwards
     */
    private static Map<String, Candidate[]> build(final Class<?> owner) {
        final Map<String, List<Candidate>> grouped = new Object2ObjectOpenHashMap<>();
        for (final Method method : owner.getDeclaredMethods()) {
            grouped.computeIfAbsent(method.getName(), ignored -> new ArrayList<>(1))
                .add(new Candidate(method, method.getParameterTypes()));
        }
        final Map<String, Candidate[]> table = new Object2ObjectOpenHashMap<>(grouped.size() * 2);
        for (final Map.Entry<String, List<Candidate>> entry : grouped.entrySet()) {
            table.put(entry.getKey(), entry.getValue().toArray(new Candidate[0]));
        }
        return table;
    }

    /** One declared method with the parameter types the table was built from. */
    public record Candidate(Method method, Class<?>[] parameterTypes) {
    }

}
