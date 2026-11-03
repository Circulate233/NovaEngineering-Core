package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

import java.util.function.ObjIntConsumer;

@SuppressWarnings("unused")
public interface Reference2IntBiMap<K> extends BiMap<K, Integer> {
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
    @NotNull
    Int2ReferenceBiMap<K> inverse();

    @Override
    @NotNull
    IntSet values();

    @Override
    @NotNull
    ReferenceSet<K> keySet();

    ObjectSet<Reference2IntMap.Entry<K>> reference2IntEntrySet();
}
