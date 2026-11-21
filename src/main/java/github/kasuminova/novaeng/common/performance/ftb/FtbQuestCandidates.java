package github.kasuminova.novaeng.common.performance.ftb;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One-shot expansion. Candidate lists may escape through third-party enumeration callbacks.
 */
public final class FtbQuestCandidates {
    private FtbQuestCandidates() {
    }

    public static <T, L extends List<T>> void appendFresh(final Supplier<L> factory,
                                                          final Consumer<L> enumerate,
                                                          final Predicate<T> filter,
                                                          final List<T> output) {
        final L candidates = factory.get();
        // Never clear, recycle, or memoize this list: an item can retain the callback argument.
        enumerate.accept(candidates);
        final int originalSize = output.size();
        boolean complete = false;
        try {
            for (final T candidate : candidates) {
                if (filter.test(candidate)) {
                    output.add(candidate);
                }
            }
            complete = true;
        } finally {
            // FilterBase publishes nothing to the caller when enumeration/filtering throws.
            if (!complete) {
                output.subList(originalSize, output.size()).clear();
            }
        }
    }
}
