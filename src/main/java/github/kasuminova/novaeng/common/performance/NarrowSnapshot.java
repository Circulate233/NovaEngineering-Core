package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds the renderer's final concrete snapshot, without an intermediate filtered collection.
 */
public final class NarrowSnapshot {
    private NarrowSnapshot() {
    }

    public static <T> ArrayList<T> arrayListOfType(final Collection<T> source, final Class<? extends T> type) {
        // Custom collections may give toArray special behavior. Preserve that original boundary.
        if (source.getClass() != ArrayList.class && source.getClass() != ObjectArrayList.class) return null;
        final List<T> list = (List<T>) source;
        // The intercepted NEW has an ArrayList return descriptor. This is the final
        // result consumed by the renderer, so another collection and toArray copy are unnecessary.
        ArrayList<T> selected = null;
        for (int index = 0, size = list.size(); index < size; index++) {
            final T value = list.get(index);
            if (type.isInstance(value)) {
                if (selected == null) selected = new ArrayList<>(1);
                selected.add(value);
            }
        }
        return selected == null ? new ArrayList<>(0) : selected;
    }
}
