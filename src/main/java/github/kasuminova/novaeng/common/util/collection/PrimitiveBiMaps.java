package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Map;
import java.util.Objects;

/**
 * Primitive access for legacy BiMap-typed fields. Fallbacks preserve externally supplied maps.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class PrimitiveBiMaps {
    private PrimitiveBiMaps() {
    }

    public static long modificationVersion(final BiMap map) {
        if (map instanceof Int2ObjectBiMap<?> typed) return typed.modificationVersion();
        if (map instanceof Object2IntBiMap<?> typed) return typed.modificationVersion();
        if (map instanceof Int2IntBiMap typed) return typed.modificationVersion();
        return -1;
    }

    public static Object getIntKey(final Map map, final int key) {
        if (map instanceof Int2ObjectBiMap<?> typed) return typed.get(key);
        if (map instanceof Int2IntBiMap typed) return typed.getBoxed(key);
        return map.get(key);
    }

    public static int getIntValue(final Map map, final Object key) {
        if (map instanceof Object2IntBiMap<?> typed) return typed.getIntRequired(key);
        if (map instanceof Object2IntMap<?> typed) {
            if (!typed.containsKey(key)) throw new NullPointerException("null integer mapping");
            return typed.getInt(key);
        }
        return (Integer) map.get(key);
    }

    @SuppressWarnings("unused")
    public static int getInts(final Map map, final int key) {
        if (map instanceof Int2IntBiMap typed) return typed.getRequired(key);
        return (Integer) getIntKey(map, key);
    }

    public static boolean containsIntKey(final Map map, final int key) {
        if (map instanceof Int2ObjectBiMap<?> typed) return typed.containsKey(key);
        if (map instanceof Int2IntBiMap typed) return typed.containsKey(key);
        return map.containsKey(key);
    }

    public static Object getIntKeyOrDefault(final Map map, final int key, final Object fallback) {
        if (map instanceof Int2ObjectBiMap typed) return typed.getOrDefault(key, fallback);
        return map.getOrDefault(key, fallback);
    }

    public static int getIntValueOrDefault(final Map map, final Object key, final int fallback) {
        if (map instanceof Object2IntBiMap<?> typed) return typed.getIntOrDefault(key, fallback);
        return (Integer) map.getOrDefault(key, fallback);
    }

    public static void putIntObject(final Map map, final int key, final Object value) {
        if (map instanceof Int2ObjectBiMap typed) typed.putDiscard(key, value, false);
        else map.put(key, value);
    }

    public static void putObjectInt(final Map map, final Object key, final int value) {
        if (map instanceof Object2IntBiMap typed) typed.putDiscard(key, value, false);
        else if (map instanceof Object2IntMap typed) typed.put(key, value);
        else map.put(key, value);
    }

    /**
     * Moves a fluid's numeric mapping, including legacy missing/null mappings.
     */
    public static void moveObjectIntKey(final BiMap map, final Object oldKey, final Object newKey) {
        if (map instanceof Object2IntBiMap typed
            && typed.containsKey(oldKey)
            && !(typed.inverse().containsKey(null)
            && Objects.equals(typed.inverse().get(null), oldKey))) {
            final int id = typed.removeIntRequired(oldKey);
            typed.putDiscard(newKey, id, false);
        } else {
            // The native branch reaches this path only for a missing or null numeric value.
            final Object value = map.remove(oldKey);
            map.put(newKey, value);
        }
    }

    public static void forcePutObjectInt(final BiMap map, final Object key, final int value) {
        if (map instanceof Object2IntBiMap typed) typed.putDiscard(key, value, true);
        else map.forcePut(key, value);
    }

    public static int entryIntValue(final Map.Entry entry) {
        if (entry instanceof Int2IntMap.Entry typed) return typed.getIntValue();
        if (entry instanceof Object2IntMap.Entry<?> typed) return typed.getIntValue();
        return (Integer) entry.getValue();
    }
}
