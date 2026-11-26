package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Map;

/**
 * Capacity policy only for the private maps constructed by HEI Ingredients.
 */
public final class IngredientMaps {
    private IngredientMaps() {
    }

    public static <K, V> V put(final Map<K, V> map, final K type, final V groups) {
        if (map.size() == 1 && !map.containsKey(type)
            && map instanceof Reference2ObjectOpenHashMap<?, ?> identityMap) {
            // The default HEI constructor reserves 16 entries (32 buckets). Restore that
            // layout before a second identity key is inserted. Rehashing the first key
            // alone cannot change its order relative to any other key, even with null.
            identityMap.ensureCapacity(16);
        }
        return map.put(type, groups);
    }
}
