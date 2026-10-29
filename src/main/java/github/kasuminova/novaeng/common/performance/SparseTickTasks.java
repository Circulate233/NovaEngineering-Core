package github.kasuminova.novaeng.common.performance;

import com.google.common.collect.Multimap;

import java.util.Collection;

public final class SparseTickTasks {

    private SparseTickTasks() {
    }

    public static <K, V> Collection<V> existing(final Multimap<K, V> tasks, final K key) {
        return tasks.asMap().get(key);
    }

    public static <K, V> void mergePending(final Multimap<K, V> tasks, final Multimap<K, V> pending) {
        if (!pending.isEmpty()) {
            tasks.putAll(pending);
            pending.clear();
        }
    }
}
