package github.kasuminova.novaeng.client.geometry;

import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bake workers share indices only until their owning reload has finished.
 */
public final class GeometryGeneration implements AutoCloseable {
    private static volatile GeometryGeneration active;
    private final GeometryGeneration previous;
    // Parallel bake workers need atomic interning and safe reads during close; fastutil maps are not concurrent.
    private final ConcurrentHashMap<Class<?>, Attachment<?>> attachments = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Object, Object> records = new ConcurrentHashMap<>();

    private GeometryGeneration(GeometryGeneration previous) {
        this.previous = previous;
    }

    public static synchronized GeometryGeneration begin() {
        return active = new GeometryGeneration(active);
    }

    public static void endCurrent() {
        final GeometryGeneration generation = active;
        if (generation != null) generation.close();
    }

    @SuppressWarnings("unchecked")
    public static <T> T intern(T value) {
        var generation = active;
        var index = generation == null ? null : generation.records;
        if (index == null) return value;
        Object prior = index.putIfAbsent(value, value);
        return prior == null ? value : (T) prior;
    }

    public static <T> T attachment(final Class<T> key, final Supplier<T> factory, final Consumer<T> finish) {
        final GeometryGeneration generation = active;
        if (generation == null) return null;
        final Attachment<?> existing = generation.attachments.get(key);
        if (existing != null) return key.cast(existing.value());
        synchronized (generation.attachments) {
            if (generation.records == null) return null;
            return key.cast(generation.attachments.computeIfAbsent(key, ignored -> new Attachment<>(factory.get(), finish)).value());
        }
    }

    @Override
    public void close() {
        final int count;
        synchronized (GeometryGeneration.class) {
            if (records == null) return;
            count = records.size();
            records = null;
            while (active != null && active.records == null) active = active.previous;
        }
        synchronized (attachments) {
            try {
                attachments.values().forEach(Attachment::seal);
            } finally {
                attachments.clear();
            }
        }
        LogManager.getLogger("novaeng_core").info("Compact geometry bake: released index for {} immutable records", count);
    }

    private record Attachment<T>(T value, Consumer<T> finish) {
        void seal() {
            finish.accept(value);
        }
    }
}
