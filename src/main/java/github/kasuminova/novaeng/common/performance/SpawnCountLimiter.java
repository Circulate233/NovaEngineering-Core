package github.kasuminova.novaeng.common.performance;

import java.util.List;
import java.util.function.Predicate;

public final class SpawnCountLimiter {

    private SpawnCountLimiter() {
    }

    public static <T> boolean isWithinLimit(final List<T> entities, final Predicate<? super T> matches,
                                            final int limit) {
        if (limit < 0) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < entities.size(); i++) {
            if (matches.test(entities.get(i)) && ++count > limit) {
                return false;
            }
        }
        return true;
    }
}
