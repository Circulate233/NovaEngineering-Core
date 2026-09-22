package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Keeps the reflective arrays the script compiler asks for once per method instead of once per call.
 *
 * <p>ZenScript's method wrapper reads a method's generic parameter types and parameter annotations inside its
 * per-parameter loop, and the JDK builds a fresh array for each of those calls. The contents cannot change for a
 * loaded method, so they are computed once and handed out afterwards.</p>
 *
 * <p>Keyed by {@link Method} identity, which is what the JDK hands back for a given method, and accessed only
 * while scripts are compiled. That is a single thread: the compiler's own type table
 * ({@code TypeRegistry.types}) is a plain {@code HashMap} written through the same path, so this cache inherits
 * the same assumption rather than being stricter than the code it serves.</p>
 *
 * <p>Entries hold the arrays the JDK produced, so callers must treat them as read-only — which the compiler
 * does. The set of methods a script can reach is fixed by the loaded API surface, so the cache stays bounded.</p>
 */
public final class MethodMetadataCache {

    private static final Map<Method, Type[]> GENERIC_PARAMETER_TYPES = new Reference2ObjectOpenHashMap<>();
    private static final Map<Method, Annotation[][]> PARAMETER_ANNOTATIONS = new Reference2ObjectOpenHashMap<>();

    private MethodMetadataCache() {
    }

    /**
     * Returns the method's generic parameter types.
     *
     * @param method method to describe
     * @return shared array of generic parameter types; callers must not modify it
     */
    public static Type[] genericParameterTypes(final Method method) {
        final Type[] cached = GENERIC_PARAMETER_TYPES.get(method);
        if (cached != null) {
            return cached;
        }
        final Type[] computed = method.getGenericParameterTypes();
        GENERIC_PARAMETER_TYPES.put(method, computed);
        return computed;
    }

    /**
     * Returns the method's parameter annotations.
     *
     * @param method method to describe
     * @return shared array of parameter annotations; callers must not modify it
     */
    public static Annotation[][] parameterAnnotations(final Method method) {
        final Annotation[][] cached = PARAMETER_ANNOTATIONS.get(method);
        if (cached != null) {
            return cached;
        }
        final Annotation[][] computed = method.getParameterAnnotations();
        PARAMETER_ANNOTATIONS.put(method, computed);
        return computed;
    }

}
