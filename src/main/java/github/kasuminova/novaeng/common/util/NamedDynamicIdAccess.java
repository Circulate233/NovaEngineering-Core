package github.kasuminova.novaeng.common.util;

public interface NamedDynamicIdAccess {
    /**
     * Returns -1 when the class has no registered ID.
     */
    int nova$getNamedDynamicId(Class<?> type);
}
