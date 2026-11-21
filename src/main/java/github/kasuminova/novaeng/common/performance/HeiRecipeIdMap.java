package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Primitive recipe IDs behind Table's ordinary Map API, including null and raw-key behavior.
 */
public final class HeiRecipeIdMap<V> extends AbstractMap<Object, V> {
    private final Long2ObjectOpenHashMap<V> ids = new Long2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<Object, V> otherKeys;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V> Map<Long, V> create() {
        // The erased Map implementation also accepts null and non-Long keys, just as HEI's
        // original Object2ObjectOpenHashMap did. Normal recipe registrations only use longs.
        return (Map) new HeiRecipeIdMap<V>();
    }

    @Override
    public V get(final Object key) {
        if (key instanceof Long id) return ids.get(id.longValue());
        return otherKeys == null ? null : otherKeys.get(key);
    }

    @Override
    public boolean containsKey(final Object key) {
        if (key instanceof Long id) return ids.containsKey(id.longValue());
        return otherKeys != null && otherKeys.containsKey(key);
    }

    @Override
    public V put(final Object key, final V value) {
        if (key instanceof Long id) return ids.put(id.longValue(), value);
        if (otherKeys == null) otherKeys = new Object2ObjectOpenHashMap<>();
        return otherKeys.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        if (key instanceof Long id) return ids.remove(id.longValue());
        return otherKeys == null ? null : otherKeys.remove(key);
    }

    @Override
    public int size() {
        return ids.size() + (otherKeys == null ? 0 : otherKeys.size());
    }

    @Override
    public void clear() {
        ids.clear();
        if (otherKeys != null) otherKeys.clear();
    }

    public void trim() {
        ids.trim();
        if (otherKeys != null) otherKeys.trim();
    }

    @Override
    public Set<Entry<Object, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return HeiRecipeIdMap.this.size();
            }

            @Override
            public void clear() {
                HeiRecipeIdMap.this.clear();
            }

            @Override
            public boolean contains(final Object value) {
                return value instanceof Entry<?, ?> entry && containsKey(entry.getKey())
                    && Objects.equals(get(entry.getKey()), entry.getValue());
            }

            @Override
            public boolean remove(final Object value) {
                return value instanceof Entry<?, ?> entry
                    && HeiRecipeIdMap.this.remove(entry.getKey(), entry.getValue());
            }

            @Override
            public Iterator<Entry<Object, V>> iterator() {
                final Iterator<Long2ObjectMap.Entry<V>> primitive = ids.long2ObjectEntrySet().iterator();
                final Iterator<Entry<Object, V>> other = otherKeys == null
                    ? Collections.emptyIterator() : otherKeys.entrySet().iterator();
                return new Iterator<>() {
                    private Iterator<?> last;

                    @Override
                    public boolean hasNext() {
                        return primitive.hasNext() || other.hasNext();
                    }

                    @Override
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    public Entry<Object, V> next() {
                        if (primitive.hasNext()) {
                            last = primitive;
                            return (Entry) primitive.next();
                        }
                        final Entry<Object, V> entry = other.next();
                        last = other;
                        return entry;
                    }

                    @Override
                    public void remove() {
                        if (last == null) throw new IllegalStateException();
                        last.remove();
                        last = null;
                    }
                };
            }
        };
    }
}
