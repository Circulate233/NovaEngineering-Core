package github.kasuminova.novaeng.client.resource;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
    private volatile Generation activeGeneration;

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
        final Generation generation = this.activeGeneration;
        return generation == null ? null : generation.answers;
    }

    /**
     * Uses one directory listing even for distinct missing paths; uncertain paths retain the live probe.
     */
    public boolean exists(final File root, final File file) {
        final Generation generation = this.activeGeneration;
        if (generation == null) {
            return file.exists();
        }
        final String path = file.getAbsolutePath();
        final DirectoryResourceSnapshot snapshot = generation.snapshot(root);
        final Boolean indexed = snapshot.contains(path);
        if (indexed != null) {
            return indexed;
        }
        final Boolean known = generation.answers.get(path);
        if (known != null) {
            return known;
        }
        final boolean exists = file.exists();
        generation.answers.put(path, exists);
        return exists;
    }

    /** BASE resource names already contain the snapshot key; ordinary probes need no File or replacement String. */
    public boolean existsResource(final File root, final String name) {
        final Generation generation = this.activeGeneration;
        if (generation != null) {
            final DirectoryResourceSnapshot snapshot = generation.snapshot(root);
            final Boolean indexed = snapshot.containsResource(name);
            if (indexed != null) return indexed;
        }
        // Retain BASE's global literal replacement, File normalization, and live behavior outside a reload.
        return exists(root, new File(root, name.replace("assets/", "")));
    }

    /** Starts a generation, discarding everything remembered before the outermost one. */
    public void begin() {
        if (this.depth.incrementAndGet() == 1) {
            this.activeGeneration = new Generation();
        }
    }

    /** Ends the current generation, releasing every recorded answer once the outermost one finishes. */
    public void end() {
        if (this.depth.updateAndGet(current -> current > 0 ? current - 1 : 0) == 0) {
            this.activeGeneration = null;
        }
    }

    private static final class Generation {
        private final Map<String, Boolean> answers = new ConcurrentHashMap<>();
        // Windows File.hashCode lowercases its path on every query; the comparator keeps the same root equivalence without that allocation.
        private final Map<String, DirectoryResourceSnapshot> directories = File.separatorChar == '\\'
            ? new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER) : new ConcurrentHashMap<>();

        private DirectoryResourceSnapshot snapshot(final File root) {
            final String path = root.getPath();
            final DirectoryResourceSnapshot existing = directories.get(path);
            if (existing != null) return existing;
            // Only the first lookup for a root takes this lock; concurrent misses must not build duplicate directory walks.
            synchronized (directories) {
                final DirectoryResourceSnapshot raced = directories.get(path);
                if (raced != null) return raced;
                final DirectoryResourceSnapshot created = new DirectoryResourceSnapshot(root);
                directories.put(path, created);
                return created;
            }
        }
    }
}
