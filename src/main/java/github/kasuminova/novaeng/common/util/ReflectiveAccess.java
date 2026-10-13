package github.kasuminova.novaeng.common.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class ReflectiveAccess {

    private ReflectiveAccess() {
    }

    public static MethodHandle getter(final Class<?> owner, final String name, final Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findGetter(owner, name, type);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static VarHandle field(final Class<?> owner, final String name, final Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findVarHandle(owner, name, type);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
