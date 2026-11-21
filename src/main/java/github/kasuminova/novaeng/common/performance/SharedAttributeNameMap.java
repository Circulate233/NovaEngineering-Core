package github.kasuminova.novaeng.common.performance;

import org.jspecify.annotations.NonNull;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ordered private name index: repeated registration sequences share their key layout.
 */
public final class SharedAttributeNameMap<V> extends AbstractMap<String, V> {
    private static final int MAX_NAMES = 128;
    private static final int MAX_LAYOUTS = 4096;
    private static final int MAX_LAYOUT_STORAGE = 131072;
    private static final Object[] EMPTY_VALUES = new Object[0];
    private static final Layout EMPTY_LAYOUT = new Layout(new String[0], new String[0], new byte[0]);
    // Concurrent reads and atomic publication are required; fastutil has no concurrent map.
    private static final ConcurrentHashMap<Transition, Layout> LAYOUTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Transition> LOOKUP = ThreadLocal.withInitial(Transition::new);
    private static int layoutStorage;
    private static volatile boolean layoutsClosed;

    private Layout layout = EMPTY_LAYOUT;
    private Object[] values = EMPTY_VALUES;
    // Public entry views require JDK fail-fast iterators and detached, writable entries:
    // removing an entry must not redirect a retained Entry.setValue to a shifted hash slot.
    private LinkedHashMap<String, V> expanded;
    private int changes;
    private Collection<V> valueView;
    private Set<Entry<String, V>> entryView;

    private static Layout append(final Layout parent, final String name) {
        if (parent.names.length >= MAX_NAMES) return null;
        final Transition lookup = LOOKUP.get();
        lookup.parent = parent;
        lookup.name = name;
        try {
            final Layout known = LAYOUTS.get(lookup);
            if (known != null) return known;
            if (layoutsClosed) return null;
            synchronized (LAYOUTS) {
                final Layout concurrent = LAYOUTS.get(lookup);
                if (concurrent != null) return concurrent;
                if (layoutsClosed) return null;
                final int count = parent.names.length + 1;
                final int tableSize = Integer.highestOneBit(count * 2 - 1) << 1;
                if (LAYOUTS.size() >= MAX_LAYOUTS || layoutStorage + count + tableSize > MAX_LAYOUT_STORAGE) {
                    layoutsClosed = true;
                    return null;
                }
                final String[] names = Arrays.copyOf(parent.names, count);
                names[count - 1] = name;
                final String[] table = new String[tableSize];
                final byte[] positions = new byte[tableSize];
                for (int index = 0; index < count; index++) {
                    int slot = hash(names[index]) & (tableSize - 1);
                    while (table[slot] != null) slot = (slot + 1) & (tableSize - 1);
                    table[slot] = names[index];
                    positions[slot] = (byte) index;
                }
                final Layout result = new Layout(names, table, positions);
                final Transition key = new Transition();
                key.parent = parent;
                key.name = name;
                LAYOUTS.put(key, result);
                layoutStorage += count + tableSize;
                return result;
            }
        } finally {
            // The per-thread probe must not keep an otherwise unused schema alive.
            lookup.parent = null;
            lookup.name = null;
        }
    }

    private static int hash(final Object key) {
        final int hash = key.hashCode();
        return hash ^ (hash >>> 16);
    }

    @Override
    public int size() {
        return expanded == null ? layout.names.length : expanded.size();
    }

    @Override
    public boolean containsKey(final Object key) {
        return expanded == null ? layout.find(key) >= 0 : expanded.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(final Object key) {
        if (expanded != null) return expanded.get(key);
        final int index = layout.find(key);
        return index < 0 ? null : (V) values[index];
    }

    @Override
    public boolean containsValue(final Object value) {
        if (expanded != null) return expanded.containsValue(value);
        for (int index = 0; index < layout.names.length; index++) {
            if (Objects.equals(value, values[index])) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V put(final String key, final V value) {
        if (expanded != null) {
            if (!expanded.containsKey(key)) changes++;
            return expanded.put(key, value);
        }
        final int previous = layout.find(key);
        if (previous >= 0) {
            final V old = (V) values[previous];
            values[previous] = value;
            return old;
        }
        // LowerStringMap always supplies non-null strings. Keep the helper's full
        // Map contract too: unsupported schemas, including null keys, expand.
        final Layout next = key == null ? null : append(layout, key);
        if (next == null) {
            expand();
            return put(key, value);
        }
        final int index = layout.names.length;
        if (index == values.length) {
            values = Arrays.copyOf(values, values.length == 0 ? 16 : values.length * 2);
        }
        values[index] = value;
        layout = next;
        changes++;
        return null;
    }

    @Override
    public V remove(final Object key) {
        if (!containsKey(key)) return null;
        expand();
        changes++;
        return expanded.remove(key);
    }

    @Override
    public void clear() {
        changes++;
        if (expanded != null) {
            expanded.clear();
        } else {
            Arrays.fill(values, 0, layout.names.length, null);
            layout = EMPTY_LAYOUT;
        }
    }

    @Override
    public @NonNull Collection<V> values() {
        if (valueView == null) valueView = new AbstractCollection<>() {
            @Override
            public int size() {
                return SharedAttributeNameMap.this.size();
            }

            @Override
            public void clear() {
                SharedAttributeNameMap.this.clear();
            }

            @Override
            public boolean contains(final Object value) {
                return containsValue(value);
            }

            @Override
            public @NonNull Iterator<V> iterator() {
                if (expanded != null) {
                    final Iterator<Entry<String, V>> entries = entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return entries.hasNext();
                        }

                        @Override
                        public V next() {
                            return entries.next().getValue();
                        }

                        @Override
                        public void remove() {
                            entries.remove();
                        }
                    };
                }
                final Layout iterationLayout = layout;
                return new Iterator<>() {
                    private int expectedChanges = changes;
                    private int cursor;
                    private int last = -1;

                    @Override
                    public boolean hasNext() {
                        return cursor < iterationLayout.names.length;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public V next() {
                        checkChanges();
                        if (!hasNext()) throw new NoSuchElementException();
                        last = cursor++;
                        return expanded == null ? (V) values[last] : expanded.get(iterationLayout.names[last]);
                    }

                    @Override
                    public void remove() {
                        if (last < 0) throw new IllegalStateException();
                        checkChanges();
                        SharedAttributeNameMap.this.remove(iterationLayout.names[last]);
                        expectedChanges = changes;
                        last = -1;
                    }

                    private void checkChanges() {
                        if (changes != expectedChanges) throw new ConcurrentModificationException();
                    }
                };
            }
        };
        return valueView;
    }

    @Override
    public @NonNull Set<Entry<String, V>> entrySet() {
        expand();
        if (entryView == null) entryView = new AbstractSet<>() {
            @Override
            public int size() {
                return expanded.size();
            }

            @Override
            public void clear() {
                SharedAttributeNameMap.this.clear();
            }

            @Override
            public @NonNull Iterator<Entry<String, V>> iterator() {
                final Iterator<Entry<String, V>> entries = expanded.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return entries.hasNext();
                    }

                    @Override
                    public Entry<String, V> next() {
                        return entries.next();
                    }

                    @Override
                    public void remove() {
                        entries.remove();
                        changes++;
                    }
                };
            }
        };
        return entryView;
    }

    @SuppressWarnings("unchecked")
    private void expand() {
        if (expanded != null) return;
        expanded = new LinkedHashMap<>(Math.max(16, (layout.names.length * 4 / 3) + 1));
        for (int index = 0; index < layout.names.length; index++) {
            expanded.put(layout.names[index], (V) values[index]);
        }
        layout = null;
        values = null;
    }

    private record Layout(String[] names, String[] table, byte[] positions) {
        private int find(final Object name) {
            if (name == null || table.length == 0) return -1;
            int slot = hash(name) & (table.length - 1);
            while (table[slot] != null) {
                if (name.equals(table[slot])) return positions[slot] & 0xFF;
                slot = (slot + 1) & (table.length - 1);
            }
            return -1;
        }
    }

    private static final class Transition {
        private Layout parent;
        private String name;

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(parent) + name.hashCode();
        }

        @Override
        public boolean equals(final Object value) {
            return value instanceof Transition other && parent == other.parent && name.equals(other.name);
        }
    }
}
