package github.kasuminova.novaeng.common.util;

import java.lang.ref.WeakReference;

/**
 * Remembers an object's identity without extending the lifetime of its object graph.
 */
public final class WeakIdentityMemo<T> {
    private WeakReference<T> reference;

    /**
     * Returns true when a non-null identity is observed for the first time since reset.
     */
    public boolean select(final T value) {
        if (value == null) {
            clear();
            return false;
        }
        if (reference != null && reference.refersTo(value)) {
            return false;
        }
        reference = new WeakReference<>(value);
        return true;
    }

    /**
     * A different world unloading must not invalidate the currently selected world.
     */
    public void clearIf(final T value) {
        if (reference != null && reference.refersTo(value)) {
            clear();
        }
    }

    public void clear() {
        if (reference != null) {
            reference.clear();
            reference = null;
        }
    }
}
