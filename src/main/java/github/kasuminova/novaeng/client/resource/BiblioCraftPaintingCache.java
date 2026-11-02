package github.kasuminova.novaeng.client.resource;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps BiblioCraft's jar-painting directory result for one model reload generation.
 *
 * <p>This is deliberately narrower than a resource-pack index. The BiblioCraft method
 * still performs the original lookup and error handling on a cache miss; only its returned array,
 * including a {@code null} negative result, is remembered. Resource-pack enumeration and the
 * later image-size reads remain in BiblioCraft's original {@code updateCustomArtDatas} method.</p>
 */
public final class BiblioCraftPaintingCache {

    private static final ThreadLocal<Deque<Generation>> GENERATIONS = new ThreadLocal<>();

    private BiblioCraftPaintingCache() {
    }

    /** Starts a fresh cache generation for the current reload call. */
    public static void beginGeneration() {
        Deque<Generation> generations = GENERATIONS.get();
        if (generations == null) {
            generations = new ArrayDeque<>();
            GENERATIONS.set(generations);
        }
        generations.push(new Generation());
    }

    /** Ends the current cache generation and restores an enclosing one, if present. */
    public static void endGeneration() {
        final Deque<Generation> generations = GENERATIONS.get();
        if (generations == null || generations.isEmpty()) {
            return;
        }

        generations.pop();
        if (generations.isEmpty()) {
            GENERATIONS.remove();
        }
    }

    /**
     * Returns whether a result has already been recorded in the current reload generation.
     */
    public static boolean hasCachedResult() {
        final Deque<Generation> generations = GENERATIONS.get();
        return generations != null && !generations.isEmpty() && generations.peek().initialized;
    }

    /**
     * Returns the recorded result for the current reload generation.
     */
    public static String[] getCachedResult() {
        final Deque<Generation> generations = GENERATIONS.get();
        if (generations == null || generations.isEmpty()) {
            return null;
        }
        return generations.peek().paintings;
    }

    /**
     * Records the original result for the current reload generation.
     */
    public static void cacheResult(final String[] paintings) {
        final Deque<Generation> generations = GENERATIONS.get();
        if (generations == null || generations.isEmpty()) {
            return;
        }
        final Generation generation = generations.peek();
        synchronized (generation) {
            if (!generation.initialized) {
                generation.paintings = paintings;
                generation.initialized = true;
            }
        }
    }

    private static final class Generation {
        private String[] paintings;
        private boolean initialized;
    }
}
