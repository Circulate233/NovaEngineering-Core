package github.kasuminova.novaeng.common.performance;

import java.util.ArrayList;
import java.util.List;

public final class IngredientGroups {
    private IngredientGroups() {
    }

    /** A mutable, detached list is required by third-party ingredient helpers. */
    public static <T> List<T> flatten(final List<? extends List<? extends T>> groups) {
        int size = 0;
        for (final List<? extends T> group : groups) {
            size = Math.addExact(size, group.size());
        }
        final List<T> result = new ArrayList<>(size);
        for (final List<? extends T> group : groups) {
            for (final T value : group) {
                result.add(value);
            }
        }
        return result;
    }
}
