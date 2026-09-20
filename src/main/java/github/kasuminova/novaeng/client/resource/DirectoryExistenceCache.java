package github.kasuminova.novaeng.client.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memoises file presence answers for the duration of one model reload.
 *
 * <p>Directory backed resource packs answer every probe by asking the filesystem, and a model reload probes
 * the same paths repeatedly. The set of files inside an author supplied resource directory does not change
 * while a reload is running, so an answer recorded during the generation stays valid until it ends. When no
 * generation is active, callers fall back to a live filesystem check and nothing is remembered.</p>
 *
 * <p>Nesting is tolerated through a depth counter rather than being rejected. A reload triggered from inside
 * another reload would otherwise abort the outer reload with an exception, which is a far worse outcome than
 * serving a slightly longer lived answer.</p>
 */
public final class DirectoryExistenceCache {

    private static final DirectoryExistenceCache INSTANCE = new DirectoryExistenceCache();

    private final AtomicInteger depth = new AtomicInteger();
    private volatile Map<String, Boolean> activeGeneration;

    private DirectoryExistenceCache() {
    }

    /**
     * Returns the shared cache used by resource pack mixins.
     *
     * @return shared cache instance
     */
    public static DirectoryExistenceCache instance() {
        return INSTANCE;
    }

    /**
     * Returns the active generation, or {@code null} when answers must not be remembered.
     *
     * @return active generation map, or {@code null}
     */
    public Map<String, Boolean> active() {
        return this.activeGeneration;
    }

    /** Starts a generation, discarding everything remembered before the outermost one. */
    public void begin() {
        if (this.depth.incrementAndGet() == 1) {
            this.activeGeneration = new ConcurrentHashMap<>();
        }
    }

    /** Ends the current generation, releasing every recorded answer once the outermost one finishes. */
    public void end() {
        if (this.depth.updateAndGet(current -> current > 0 ? current - 1 : 0) == 0) {
            this.activeGeneration = null;
        }
    }
}
