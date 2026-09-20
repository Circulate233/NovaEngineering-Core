package github.kasuminova.novaeng.client.resource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Keeps BiblioCraft's jar-painting directory result for one model reload generation.
 *
 * <p>This is deliberately narrower than a resource-pack index. The wrapped BiblioCraft method
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
     * Returns the cached jar scan when a reload generation is active, otherwise delegates live.
     *
     * <p>The supplier is intentionally not wrapped in a catch block. Unchecked failures from the
     * production method retain their original propagation semantics, while a normal {@code null}
     * return is recorded as a negative result for this generation.</p>
     *
     * @param loader original BiblioCraft jar scan
     * @return the original array, or its original {@code null} result
     */
    public static String[] getOrCompute(final Supplier<String[]> loader) {
        final Deque<Generation> generations = GENERATIONS.get();
        if (generations == null || generations.isEmpty()) {
            return loader.get();
        }

        final Generation generation = generations.peek();
        if (generation.initialized) {
            return generation.paintings;
        }

        synchronized (generation) {
            if (!generation.initialized) {
                generation.paintings = loader.get();
                generation.initialized = true;
            }
            return generation.paintings;
        }
    }

    private static final class Generation {
        private String[] paintings;
        private boolean initialized;
    }
}
