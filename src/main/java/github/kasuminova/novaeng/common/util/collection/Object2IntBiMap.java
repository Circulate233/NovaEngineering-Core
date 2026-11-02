package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.function.ObjIntConsumer;

public interface Object2IntBiMap<K> extends BiMap<K, Integer> {
    int getInt(Object key);

    int getIntOrDefault(Object key, int fallback);

    int getIntRequired(Object key);

    boolean containsValue(int value);

    int put(K key, int value);

    int forcePut(K key, int value);

    void putDiscard(K key, int value, boolean force);

    int removeInt(Object key);

    int removeIntRequired(Object key);

    long modificationVersion();

    void forEachObjectInt(ObjIntConsumer<? super K> consumer);

    @Override
    Int2ObjectBiMap<K> inverse();

    @Override
    IntSet values();

    @Override
    ObjectSet<K> keySet();

    ObjectSet<Object2IntMap.Entry<K>> object2IntEntrySet();
}
