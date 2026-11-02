package github.kasuminova.novaeng.client.model;

import net.minecraft.util.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Parsed texture identifiers shared by model bakes within a resource reload.
 */
public final class TextureLocationCache {

    private static final int MAX_ENTRIES = 16384;
    private static volatile ConcurrentMap<String, ResourceLocation> active;
    private static int depth;

    private TextureLocationCache() {
    }

    public static synchronized void begin() {
        if (depth++ == 0) {
            active = new ConcurrentHashMap<>();
        }
    }

    public static synchronized void end() {
        if (depth > 0 && --depth == 0) {
            active = null;
        }
    }

    public static ResourceLocation parse(final String name) {
        final ConcurrentMap<String, ResourceLocation> locations = active;
        if (locations == null) {
            return new ResourceLocation(name);
        }
        final ResourceLocation cached = locations.get(name);
        if (cached != null) {
            return cached;
        }
        final ResourceLocation created = new ResourceLocation(name);
        // Bound retained keys even if a failed resource reload never reaches its return hook.
        if (locations.size() < MAX_ENTRIES) {
            final ResourceLocation previous = locations.putIfAbsent(name, created);
            if (previous != null) {
                return previous;
            }
        }
        return created;
    }
}
