package github.kasuminova.novaeng.client.model.async;

import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Keeps a legacy HashMap field descriptor while routing hot operations to a lock-free map. */
public class NonBlockingHashMapFacade<K, V> extends HashMap<K, V> {

    private final NonBlockingHashMap<K, V> delegate = new NonBlockingHashMap<>();

    @Override
    public V get(final Object key) {
        return this.delegate.get(key);
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public V put(final K key, final V value) {
        return this.delegate.put(key, value);
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        return this.delegate.putIfAbsent(key, value);
    }

    @Override
    public V remove(final Object key) {
        return this.delegate.remove(key);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public @NonNull Set<K> keySet() {
        return this.delegate.keySet();
    }

    @Override
    public @NonNull Collection<V> values() {
        return this.delegate.values();
    }

    @Override
    public @NonNull Set<Map.Entry<K, V>> entrySet() {
        return this.delegate.entrySet();
    }
}
