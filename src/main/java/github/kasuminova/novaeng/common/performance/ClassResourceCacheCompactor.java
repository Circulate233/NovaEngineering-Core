package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.launchwrapper.Launch;
import top.outlands.foundation.boot.ActualClassLoader;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Shares identical raw class resources without changing cache keys or the active class loader.
 */
public final class ClassResourceCacheCompactor {
    private static final VarHandle RESOURCE_CACHE = resourceCacheHandle();
    private static final VarHandle LOADED_CLASSES = loadedClassesHandle();
    private static int lastSize;
    private static int lastLoadedSize;

    private ClassResourceCacheCompactor() {
    }

    private static VarHandle resourceCacheHandle() {
        try {
            return MethodHandles.privateLookupIn(ActualClassLoader.class, MethodHandles.lookup())
                                .findVarHandle(ActualClassLoader.class, "resourceCache", Map.class);
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access Foundation's class resource cache", error);
        }
    }

    private static VarHandle loadedClassesHandle() {
        try {
            return MethodHandles.privateLookupIn(ActualClassLoader.class, MethodHandles.lookup())
                                .findVarHandle(ActualClassLoader.class, "cachedClasses", Map.class);
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access Foundation's completed class cache", error);
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized void compactIfGrown() {
        final Map<String, byte[]> cache = (Map<String, byte[]>) RESOURCE_CACHE.get(Launch.classLoader);
        final Map<String, ?> loaded = (Map<String, ?>) LOADED_CLASSES.get(Launch.classLoader);
        if (!(cache instanceof ConcurrentMap<String, byte[]> concurrent)
            || cache.size() < lastSize + 1024 && loaded.size() < lastLoadedSize + 256) return;
        lastSize = cache.size();
        lastLoadedSize = loaded.size();
        final Savings savings = compact(concurrent, loaded);
        if (savings.aliases > 0) {
            NovaEngineeringCore.log.info("Shared {} identical class-resource aliases ({} KiB of duplicate arrays).",
                savings.aliases, savings.bytes / 1024);
        }
    }

    public static Savings compact(final ConcurrentMap<String, byte[]> cache, final Map<String, ?> loaded) {
        int aliases = 0;
        long bytes = 0;
        for (final var entry : cache.entrySet()) {
            final String name = entry.getKey();
            // Do not share aliases while a class may still be in its first transformation pipeline.
            if (name.indexOf('.') < 0 || !loaded.containsKey(name)) continue;
            final byte[] original = entry.getValue();
            final byte[] canonical = cache.get(name.replace('.', '/'));
            if (canonical != null && canonical != original && Arrays.equals(original, canonical)
                && cache.replace(name, original, canonical)) {
                aliases++;
                bytes += original.length;
            }
        }
        return new Savings(aliases, bytes);
    }

    public record Savings(int aliases, long bytes) {
    }
}
