package github.kasuminova.novaeng.common.util.collection;

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public interface Int2IntBiMap extends BiMap<Integer, Integer> {
    int get(int key);

    int getOrDefault(int key, int fallback);

    int getRequired(int key);

    Integer getBoxed(int key);

    int removeRequired(int key);

    boolean containsKey(int key);

    boolean containsValue(int value);

    int put(int key, int value);

    int forcePut(int key, int value);

    void putDiscard(int key, int value, boolean force);

    int remove(int key);

    long modificationVersion();

    void forEachIntInt(IntIntConsumer consumer);

    @Override
    Int2IntBiMap inverse();

    @Override
    IntSet keySet();

    @Override
    IntSet values();

    ObjectSet<Int2IntMap.Entry> int2IntEntrySet();

    @FunctionalInterface
    interface IntIntConsumer {
        void accept(int key, int value);
    }
}
