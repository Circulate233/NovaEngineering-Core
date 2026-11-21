package github.kasuminova.novaeng.common.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Empty private index sentinel compatible with Guava's HashMap factory signature.
 */
public final class SharedEmptyHashMap<K, V> extends HashMap<K, V> {
    private static final SharedEmptyHashMap<?, ?> EMPTY = new SharedEmptyHashMap<>();

    private SharedEmptyHashMap() {
        super(0);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> instance() {
        return (HashMap<K, V>) EMPTY;
    }

    // Empty removals retain HashMap's no-op semantics. All insertion paths fail
    // closed if another transformer attempts a write without installing an index.
    @Override
    public V put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> values) {
        if (!values.isEmpty()) throw new UnsupportedOperationException();
    }

    @Override
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }
}
