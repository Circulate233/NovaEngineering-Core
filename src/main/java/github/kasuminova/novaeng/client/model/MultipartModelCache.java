package github.kasuminova.novaeng.client.model;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;

import java.util.Map;

public final class MultipartModelCache {
    private static final ReadMostlyObjectCache<MultipartModelKey, MultipartBakedModel> CACHE =
        new ReadMostlyObjectCache<>("multipart.models", 719);
    private static volatile long generation;

    private MultipartModelCache() {
    }

    public static MultipartBakedModel create(final Map<Predicate<IBlockState>, IBakedModel> selectors) {
        final long currentGeneration = generation;
        final MultipartModelKey key = MultipartModelKey.from(selectors, currentGeneration);
        final MultipartBakedModel cached = CACHE.get(key);
        if (cached != null && currentGeneration == generation) {
            return cached;
        }
        if (cached != null) {
            return createUncached(selectors);
        }
        synchronized (CACHE.lock(key)) {
            if (currentGeneration != generation) {
                return createUncached(selectors);
            }
            final MultipartBakedModel raced = CACHE.get(key);
            if (raced != null) {
                return raced;
            }
            // Builders remain mutable. Freeze only on a miss; a hit allocates neither a model nor a map.
            final MultipartBakedModel model = createUncached(selectors);
            CACHE.put(key.snapshot(), model);
            return model;
        }
    }

    public static void clear() {
        generation++;
        CACHE.clear();
    }

    public static String capacityProbe() {
        return CACHE.report();
    }

    private static MultipartBakedModel createUncached(final Map<Predicate<IBlockState>, IBakedModel> selectors) {
        return new MultipartBakedModel(ImmutableMap.copyOf(selectors));
    }
}
