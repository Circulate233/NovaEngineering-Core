package github.kasuminova.novaeng.client.model;

import net.minecraft.util.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Parsed texture identifiers shared by model bakes within a resource reload.
 */
public final class TextureLocationCache {

    private static final int MAX_ENTRIES = 16384;
    private static final int MAX_ABSOLUTE_ENTRIES = 32768;
    private static volatile Generation active;
    private static int depth;

    private TextureLocationCache() {
    }

    public static synchronized void begin() {
        if (depth++ == 0) {
            active = new Generation();
        }
    }

    public static synchronized void end() {
        if (depth > 0 && --depth == 0) {
            active = null;
        }
    }

    public static ResourceLocation parse(final String name) {
        final Generation generation = active;
        if (generation == null) {
            return new ResourceLocation(name);
        }
        final ConcurrentMap<String, ResourceLocation> locations = generation.parsed;
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

    public static ResourceLocation absoluteTexture(final ResourceLocation sprite) {
        final String path = sprite.getPath();
        final boolean prefixed = path.startsWith("textures/");
        final boolean suffixed = path.endsWith(".png");
        if (prefixed && suffixed) {
            return sprite;
        }
        final Generation generation = active;
        if (generation != null) {
            final ResourceLocation cached = generation.absolute.get(sprite);
            if (cached != null) return cached;
        }

        final String absolutePath = (prefixed ? "" : "textures/") + path + (suffixed ? "" : ".png");
        final ResourceLocation created = new ResourceLocation(sprite.getNamespace(), absolutePath);
        if (generation != null && generation.absolute.size() < MAX_ABSOLUTE_ENTRIES) {
            final ResourceLocation previous = generation.absolute.putIfAbsent(sprite, created);
            if (previous != null) return previous;
        }
        return created;
    }

    private static final class Generation {
        private final ConcurrentMap<String, ResourceLocation> parsed = new ConcurrentHashMap<>();
        private final ConcurrentMap<ResourceLocation, ResourceLocation> absolute = new ConcurrentHashMap<>();
    }
}
