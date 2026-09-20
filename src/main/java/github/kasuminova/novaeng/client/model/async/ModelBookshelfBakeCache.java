package github.kasuminova.novaeng.client.model.async;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;

import java.util.Map;

public final class ModelBookshelfBakeCache {

    private static final Map<String, IBakedModel[][]> CACHE = new NonBlockingHashMap<>();

    private ModelBookshelfBakeCache() {
    }

    public static IBakedModel[][] get(final String key) {
        return CACHE.get(key);
    }

    public static IBakedModel[][] putIfAbsent(final String key, final IBakedModel[][] value) {
        final IBakedModel[][] existing = CACHE
            .putIfAbsent(key, value);
        return existing == null ? value : existing;
    }

    public static void clear() {
        CACHE.clear();
    }
}
