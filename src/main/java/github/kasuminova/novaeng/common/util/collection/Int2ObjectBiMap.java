package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface Int2ObjectBiMap<V> extends BiMap<Integer, V> {
    V get(int key);

    V getOrDefault(int key, V fallback);

    boolean containsKey(int key);

    V put(int key, V value);

    @SuppressWarnings("unused")
    V forcePut(int key, V value);

    void putDiscard(int key, V value, boolean force);

    V remove(int key);

    long modificationVersion();

    @SuppressWarnings("unused")
    void forEachIntObject(IntObjectConsumer<? super V> consumer);

    @Override
    @NotNull
    Object2IntBiMap<V> inverse();

    @Override
    @NotNull
    IntSet keySet();

    @Override
    @NotNull
    ObjectSet<V> values();

    ObjectSet<Int2ObjectMap.Entry<V>> int2ObjectEntrySet();

    @FunctionalInterface
    interface IntObjectConsumer<V> {
        void accept(int key, V value);
    }
}
