package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

@SuppressWarnings("unused")
public interface Int2ReferenceBiMap<V> extends BiMap<Integer, V> {
    V get(int key);

    V getOrDefault(int key, V fallback);

    boolean containsKey(int key);

    V put(int key, V value);

    V forcePut(int key, V value);

    void putDiscard(int key, V value, boolean force);

    V remove(int key);

    long modificationVersion();

    void forEachIntObject(IntObjectConsumer<? super V> consumer);

    @Override
    @NotNull
    Reference2IntBiMap<V> inverse();

    @Override
    @NotNull
    IntSet keySet();

    @Override
    @NotNull
    ReferenceSet<V> values();

    ObjectSet<Int2ReferenceMap.Entry<V>> int2ReferenceEntrySet();

    @FunctionalInterface
    interface IntObjectConsumer<V> {
        void accept(int key, V value);
    }
}
