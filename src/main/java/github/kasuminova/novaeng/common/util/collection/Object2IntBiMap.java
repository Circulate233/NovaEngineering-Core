package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.NotNull;

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

    @SuppressWarnings("unused")
    int forcePut(K key, int value);

    void putDiscard(K key, int value, boolean force);

    int removeInt(Object key);

    int removeIntRequired(Object key);

    long modificationVersion();

    @SuppressWarnings("unused")
    void forEachObjectInt(ObjIntConsumer<? super K> consumer);

    @Override
    @NotNull
    Int2ObjectBiMap<K> inverse();

    @Override
    @NotNull
    IntSet values();

    @Override
    @NotNull
    ObjectSet<K> keySet();

    ObjectSet<Object2IntMap.Entry<K>> object2IntEntrySet();
}
