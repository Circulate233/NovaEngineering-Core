/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package github.kasuminova.novaeng.common.util.collection;

import org.jetbrains.annotations.NotNull;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

/**
 * Primitive specialization of Guava 33.6.0 HashBiMap. Bucket chains, cached hashes,
 * insertion order and forward/inverse replacement rules follow the original implementation.
 */
// Backed views intentionally retain their map; fastutil supplies the inherited Cloneable marker.
@SuppressWarnings({"ReturnOfInnerClass", "CloneableClassInSecureContext"})
public final class Int2ObjectHashBiMap<V> extends AbstractMap<Integer, V>
    implements Int2ObjectBiMap<V>, Serializable {
    private final Inverse inverse = new Inverse();
    private transient Node<V>[] keyTable, valueTable;
    private transient Node<V> first, last;
    private transient int size, mask;
    private transient long modifications;
    private boolean frozen;
    private transient IntSet keyView;
    private transient ObjectSet<V> valueView;
    private transient ObjectSet<Int2ObjectMap.Entry<V>> entries;

    public Int2ObjectHashBiMap() {
        this(16);
    }

    public Int2ObjectHashBiMap(final int expectedSize) {
        init(expectedSize);
    }

    private static int smear(final int hash) {
        return Integer.rotateLeft(hash * 0xcc9e2d51, 15) * 0x1b873593;
    }

    @SuppressWarnings("unchecked")
    public static <V> Int2ObjectHashBiMap<V> copyOf(
        final Map<? extends Integer, ? extends V> source) {
        final Int2ObjectHashBiMap<V> result = new Int2ObjectHashBiMap<>(source.size());
        if (source instanceof Int2ObjectHashBiMap<?> nativeMap) {
            final Int2ObjectHashBiMap<V> typed = (Int2ObjectHashBiMap<V>) nativeMap;
            for (Node<V> node = typed.first; node != null; node = node.next)
                result.append(
                    new Node<>(
                        node.key, node.nullKey, node.keyHash, node.value, node.valueHash));
        } else result.putAll(source);
        return result;
    }

    public static <V> Int2ObjectHashBiMap<V> immutableCopyOf(
        final Map<? extends Integer, ? extends V> source) {
        final Int2ObjectHashBiMap<V> result = copyOf(source);
        for (Node<V> node = result.first; node != null; node = node.next) {
            if (node.nullKey || node.value == null) throw new NullPointerException("null mapping");
        }
        result.frozen = true;
        return result;
    }

    @SuppressWarnings("unchecked")
    private void init(final int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("negative expected size");
        final int capacity =
            (int)
                Math.min(
                    1L << 30,
                    HashCommon.nextPowerOfTwo((long) Math.max(2, expectedSize)));
        keyTable = new Node[capacity];
        valueTable = new Node[capacity];
        mask = capacity - 1;
    }

    private void checkMutable() {
        if (frozen) throw new UnsupportedOperationException();
    }

    private Node<V> findKey(final int number, final boolean isNull) {
        return seekKey(number, isNull, smear(isNull ? 0 : number));
    }

    private Node<V> findKeyBoxed(final Object value) {
        return value == null
            ? findKey(0, true)
            : value instanceof Integer number ? findKey(number, false) : null;
    }

    private Node<V> seekKey(final int number, final boolean isNull, final int hash) {
        for (Node<V> node = keyTable[hash & mask]; node != null; node = node.nextKey)
            if (node.keyHash == hash && node.nullKey == isNull && (isNull || node.key == number))
                return node;
        return null;
    }

    private Integer boxedKey(final Node<V> node) {
        return node.nullKey ? null : node.key;
    }

    private int requiredKey(final Node<V> node) {
        if (node == null || node.nullKey) throw new NullPointerException("null integer mapping");
        return node.key;
    }

    private Node<V> findValue(final Object value) {
        return seekValue(value, smear(Objects.hashCode(value)));
    }

    private Node<V> seekValue(final Object value, final int hash) {
        for (Node<V> node = valueTable[hash & mask]; node != null; node = node.nextValue)
            if (node.valueHash == hash && Objects.equals(value, node.value)) return node;
        return null;
    }

    private V boxedValue(final Node<V> node) {
        return node.value;
    }

    private void delete(final Node<V> node) {
        int bucket = node.keyHash & mask;
        Node<V> previous = null;
        for (Node<V> current = keyTable[bucket]; ; current = current.nextKey) {
            if (current == node) {
                if (previous == null) keyTable[bucket] = node.nextKey;
                else previous.nextKey = node.nextKey;
                break;
            }
            previous = current;
        }
        bucket = node.valueHash & mask;
        previous = null;
        for (Node<V> current = valueTable[bucket]; ; current = current.nextValue) {
            if (current == node) {
                if (previous == null) valueTable[bucket] = node.nextValue;
                else previous.nextValue = node.nextValue;
                break;
            }
            previous = current;
        }
        if (node.prev == null) first = node.next;
        else node.prev.next = node.next;
        if (node.next == null) last = node.prev;
        else node.next.prev = node.prev;
        node.prev = node.next = node.nextKey = node.nextValue = null;
        size--;
        modifications++;
    }

    private void index(final Node<V> node) {
        final int keyBucket = node.keyHash & mask, valueBucket = node.valueHash & mask;
        node.nextKey = keyTable[keyBucket];
        keyTable[keyBucket] = node;
        node.nextValue = valueTable[valueBucket];
        valueTable[valueBucket] = node;
        size++;
        modifications++;
    }

    private void splice(final Node<V> node, final Node<V> before, final Node<V> after) {
        index(node);
        node.prev = before;
        node.next = after;
        if (before == null) first = node;
        else before.next = node;
        if (after == null) last = node;
        else after.prev = node;
    }

    private void append(final Node<V> node) {
        splice(node, last, null);
    }

    private void replaceNodeForKey(final Node<V> oldNode, final Node<V> newNode) {
        final Node<V> before = oldNode.prev, after = oldNode.next;
        delete(oldNode);
        splice(newNode, before, after);
    }

    @SuppressWarnings("unchecked")
    private void rehashIfNecessary() {
        if (size <= keyTable.length || keyTable.length == 1 << 30) return;
        final int capacity = keyTable.length * 2;
        keyTable = new Node[capacity];
        valueTable = new Node[capacity];
        mask = capacity - 1;
        size = 0;
        for (Node<V> node = first; node != null; node = node.next) index(node);
        modifications++;
    }

    private Node<V> putForward(
        final int key, final boolean keyNull, final V value, final boolean force) {
        checkMutable();
        final int keyHash = smear(keyNull ? 0 : key), valueHash = smear(Objects.hashCode(value));
        final Node<V> oldKey = seekKey(key, keyNull, keyHash);
        if (oldKey != null
            && oldKey.valueHash == valueHash
            && (Objects.equals(value, oldKey.value))) return oldKey;
        final Node<V> oldValue = seekValue(value, valueHash);
        if (oldValue != null) {
            if (!force) throw new IllegalArgumentException("value already present");
            delete(oldValue);
        }
        final Node<V> node = new Node<>(key, keyNull, keyHash, value, valueHash);
        if (oldKey != null) {
            replaceNodeForKey(oldKey, node);
            return oldKey;
        }
        append(node);
        rehashIfNecessary();
        return null;
    }

    private Node<V> putInverse(
        final int key, final boolean keyNull, final V value, final boolean force) {
        checkMutable();
        final int valueHash = smear(Objects.hashCode(value)), keyHash = smear(keyNull ? 0 : key);
        final Node<V> oldValue = seekValue(value, valueHash);
        final Node<V> oldKey = seekKey(key, keyNull, keyHash);
        if (oldValue != null
            && oldValue.keyHash == keyHash
            && (oldValue.nullKey == keyNull && (keyNull || oldValue.key == key)))
            return oldValue;
        if (oldKey != null && !force) throw new IllegalArgumentException("key already present");
        if (oldValue != null) delete(oldValue);
        final Node<V> node = new Node<>(key, keyNull, keyHash, value, valueHash);
        if (oldKey != null) replaceNodeForKey(oldKey, node);
        else append(node);
        rehashIfNecessary();
        return oldValue;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public long modificationVersion() {
        return modifications;
    }

    @Override
    @NotNull
    public Object2IntBiMap<V> inverse() {
        return inverse;
    }

    @Override
    public boolean containsKey(final Object key) {
        return findKeyBoxed(key) != null;
    }

    @Override
    public boolean containsValue(final Object value) {
        return seekValue(value, smear(Objects.hashCode(value))) != null;
    }

    @Override
    public void clear() {
        checkMutable();
        Arrays.fill(keyTable, null);
        Arrays.fill(valueTable, null);
        first = last = null;
        size = 0;
        modifications++;
    }

    @Override
    public V get(final Object key) {
        final Node<V> node = findKeyBoxed(key);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public V getOrDefault(final Object key, final V fallback) {
        final Node<V> node = findKeyBoxed(key);
        return node == null ? fallback : boxedValue(node);
    }

    @Override
    public V put(final Integer key, final V value) {
        return putBoxed(key, value, false);
    }

    @Override
    public V forcePut(final Integer key, final V value) {
        return putBoxed(key, value, true);
    }

    private V putBoxed(final Integer key, final V value, final boolean force) {
        final Node<V> previous =
            putForward(key == null ? 0 : key, key == null, value, force);
        if (previous == null) return null;
        if (Objects.equals(value, previous.value)) return value;
        return boxedValue(previous);
    }

    @Override
    public V remove(final Object key) {
        checkMutable();
        final Node<V> node = findKeyBoxed(key);
        if (node == null) return null;
        final V value = boxedValue(node);
        delete(node);
        return value;
    }

    @Override
    public boolean containsKey(final int key) {
        return findKey(key, false) != null;
    }

    @Override
    public V get(final int key) {
        final Node<V> node = findKey(key, false);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public V getOrDefault(final int key, final V fallback) {
        final Node<V> node = findKey(key, false);
        return node == null ? fallback : boxedValue(node);
    }

    @Override
    public V put(final int key, final V value) {
        return putPrimitive(key, value, false);
    }

    @Override
    public V forcePut(final int key, final V value) {
        return putPrimitive(key, value, true);
    }

    private V putPrimitive(final int key, final V value, final boolean force) {
        final Node<V> previous = putForward(key, false, value, force);
        return previous == null
            ? null
            : Objects.equals(value, previous.value) ? value : previous.value;
    }

    @Override
    public void putDiscard(final int key, final V value, final boolean force) {
        putForward(key, false, value, force);
    }

    @Override
    public V remove(final int key) {
        checkMutable();
        final Node<V> node = findKey(key, false);
        final V result = (node == null ? null : boxedValue(node));
        if (node != null) delete(node);
        return result;
    }

    @Override
    public void putAll(final @NotNull Map<? extends Integer, ? extends V> source) {
        checkMutable();
        if (source instanceof Int2ObjectHashBiMap<?> nativeMap) {
            @SuppressWarnings("unchecked") final Int2ObjectHashBiMap<V> typed = (Int2ObjectHashBiMap<V>) nativeMap;
            for (Node<V> node = typed.first; node != null; node = node.next)
                putForward(node.key, node.nullKey, node.value, false);
            return;
        }

        for (final Map.Entry<? extends Integer, ? extends V> entry : source.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public void forEach(final BiConsumer<? super Integer, ? super V> consumer) {
        Objects.requireNonNull(consumer);
        for (Node<V> node = first; node != null; node = node.next)
            consumer.accept(boxedKey(node), boxedValue(node));
    }

    @Override
    public void replaceAll(final BiFunction<? super Integer, ? super V, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final Node<V> original = first;
        clear();
        for (Node<V> node = original; node != null; node = node.next)
            put(boxedKey(node), function.apply(boxedKey(node), boxedValue(node)));
    }

    @Override
    public V putIfAbsent(final Integer key, final V value) {
        checkMutable();
        final V old = get(key);
        return old == null ? put(key, value) : old;
    }

    @Override
    public boolean remove(final Object key, final Object value) {
        checkMutable();
        if (!containsKey(key) || !Objects.equals(get(key), value)) return false;
        remove(key);
        return true;
    }

    @Override
    public boolean replace(final Integer key, final V oldValue, final V newValue) {
        checkMutable();
        if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    public V replace(final Integer key, final V value) {
        checkMutable();
        return containsKey(key) ? put(key, value) : null;
    }

    @Override
    public V computeIfAbsent(final Integer key, @NotNull Function<? super Integer, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        V value = get(key);
        if (value == null && (value = function.apply(key)) != null) put(key, value);
        return value;
    }

    @Override
    public V computeIfPresent(
        final Integer key, @NotNull BiFunction<? super Integer, ? super V, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final V old = get(key);
        if (old == null) return null;
        final V value = function.apply(key, old);
        if (value == null) remove(key);
        else put(key, value);
        return value;
    }

    @Override
    public V compute(
        final Integer key, @NotNull BiFunction<? super Integer, ? super V, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final V value = function.apply(key, get(key));
        if (value == null) {
            if (containsKey(key)) remove(key);
        } else put(key, value);
        return value;
    }

    @Override
    public V merge(
        final Integer key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        Objects.requireNonNull(value);
        final V old = get(key);
        final V result = old == null ? value : function.apply(old, value);
        if (result == null) remove(key);
        else put(key, result);
        return result;
    }

    @Override
    public void forEachIntObject(final Int2ObjectBiMap.IntObjectConsumer<? super V> consumer) {
        Objects.requireNonNull(consumer);
        for (Node<V> node = first; node != null; node = node.next)
            consumer.accept(requiredKey(node), boxedValue(node));
    }

    @Override
    @NotNull
    public IntSet keySet() {
        if (keyView == null) {
            final IntSet view = new IntKeySet();
            keyView = frozen ? IntSets.unmodifiable(view) : view;
        }
        return keyView;
    }

    @Override
    @NotNull
    public ObjectSet<V> values() {
        if (valueView == null) {
            final ObjectSet<V> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public boolean contains(final Object value) {
                        return seekValue(value, smear(Objects.hashCode(value))) != null;
                    }

                    @Override
                    public void clear() {
                        Int2ObjectHashBiMap.this.clear();
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        final Node<V> node = findValue(value);
                        if (node == null) return false;
                        delete(node);
                        return true;
                    }

                    @Override
                    @NotNull
                    public ObjectIterator<V> iterator() {
                        return new Cursor<V>() {
                            @Override
                            V output(final Node<V> node) {
                                return boxedValue(node);
                            }
                        };
                    }
                };
            valueView = frozen ? ObjectSets.unmodifiable(view) : view;
        }
        return valueView;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    public ObjectSet<Map.Entry<Integer, V>> entrySet() {
        return (ObjectSet) int2ObjectEntrySet();
    }

    @Override
    public ObjectSet<Int2ObjectMap.Entry<V>> int2ObjectEntrySet() {
        if (entries == null) {
            final ObjectSet<Int2ObjectMap.Entry<V>> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public void clear() {
                        Int2ObjectHashBiMap.this.clear();
                    }

                    @Override
                    public boolean contains(final Object value) {
                        if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                        final Node<V> node = findKeyBoxed(entry.getKey());
                        return node != null
                            && Objects.equals(boxedValue(node), entry.getValue());
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        if (!contains(value)) return false;
                        Int2ObjectHashBiMap.this.remove(((Map.Entry<?, ?>) value).getKey());
                        return true;
                    }

                    @Override
                    @NotNull
                    public ObjectIterator<Int2ObjectMap.Entry<V>> iterator() {
                        return new Cursor<Int2ObjectMap.Entry<V>>() {
                            @Override
                            Int2ObjectMap.Entry<V> output(final Node<V> node) {
                                return new ForwardEntry(node, this);
                            }
                        };
                    }
                };
            entries = frozen ? ObjectSets.unmodifiable(view) : view;
        }
        return entries;
    }

    @Serial
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(size);
        for (Node<V> node = first; node != null; node = node.next) {
            stream.writeBoolean(node.nullKey);
            stream.writeInt(node.key);
            stream.writeObject(node.value);
        }
    }

    @SuppressWarnings("unchecked")
    @Serial
    private void readObject(final ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        final int count = stream.readInt();
        if (count < 0) throw new InvalidObjectException("negative size");
        init(count);
        final boolean wasFrozen = frozen;
        frozen = false;
        for (int i = 0; i < count; i++) {
            final boolean keyNull = stream.readBoolean();
            final int key = stream.readInt();
            final V value = (V) stream.readObject();
            putForward(key, keyNull, value, false);
        }
        frozen = wasFrozen;
    }

    private static final class Node<V> {
        final int keyHash, valueHash;
        final int key;
        final V value;
        final boolean nullKey;
        Node<V> nextKey, nextValue, prev, next;

        Node(
            final int key,
            final boolean nullKey,
            final int keyHash,
            final V value,
            final int valueHash) {
            this.key = key;
            this.value = value;
            this.keyHash = keyHash;
            this.valueHash = valueHash;
            this.nullKey = nullKey;
        }
    }

    private final class Inverse extends AbstractMap<V, Integer>
        implements Object2IntBiMap<V>, Serializable {
        private transient ObjectSet<Object2IntMap.Entry<V>> inverseEntries;

        @Override
        public int size() {
            return size;
        }

        @Override
        public long modificationVersion() {
            return modifications;
        }

        @Override
        @NotNull
        public Int2ObjectBiMap<V> inverse() {
            return Int2ObjectHashBiMap.this;
        }

        @Override
        public void clear() {
            Int2ObjectHashBiMap.this.clear();
        }

        @Override
        public boolean containsKey(final Object key) {
            return seekValue(key, smear(Objects.hashCode(key))) != null;
        }

        @Override
        public boolean containsValue(final Object value) {
            return findKeyBoxed(value) != null;
        }

        @Override
        public Integer get(final Object key) {
            final Node<V> node = findValue(key);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public Integer getOrDefault(final Object key, final Integer fallback) {
            final Node<V> node = findValue(key);
            return node == null ? fallback : boxedKey(node);
        }

        @Override
        public Integer put(final V key, final Integer value) {
            return putBoxed(key, value, false);
        }

        @Override
        public Integer forcePut(final V key, final Integer value) {
            return putBoxed(key, value, true);
        }

        private Integer putBoxed(final V key, final Integer value, final boolean force) {
            final Node<V> previous =
                putInverse(value == null ? 0 : value, value == null, key, force);
            if (previous == null) return null;
            if (value == null
                ? previous.nullKey
                : !previous.nullKey && previous.key == value) return value;
            return boxedKey(previous);
        }

        @Override
        public Integer remove(final Object key) {
            checkMutable();
            final Node<V> node = findValue(key);
            if (node == null) return null;
            final Integer value = boxedKey(node);
            delete(node);
            return value;
        }

        @Override
        public boolean containsValue(final int value) {
            return findKey(value, false) != null;
        }

        @Override
        public int getInt(final Object key) {
            return getIntOrDefault(key, 0);
        }

        @Override
        public int getIntOrDefault(final Object key, final int fallback) {
            final Node<V> node = findValue(key);
            return node == null ? fallback : requiredKey(node);
        }

        @Override
        public int getIntRequired(final Object key) {
            return requiredKey(findValue(key));
        }

        @Override
        public int put(final V key, final int value) {
            return putPrimitive(key, value, false);
        }

        @Override
        public int forcePut(final V key, final int value) {
            return putPrimitive(key, value, true);
        }

        private int putPrimitive(final V key, final int value, final boolean force) {
            final Node<V> previous = putInverse(value, false, key, force);
            return previous == null || previous.nullKey ? 0 : previous.key;
        }

        @Override
        public void putDiscard(final V key, final int value, final boolean force) {
            putInverse(value, false, key, force);
        }

        @Override
        public int removeIntRequired(final Object key) {
            checkMutable();
            final Node<V> node = findValue(key);
            final boolean missing = node == null || node.nullKey;
            final int value = node == null ? 0 : node.key;
            if (node != null) delete(node);
            if (missing) throw new NullPointerException("null integer mapping");
            return value;
        }

        @Override
        public int removeInt(final Object key) {
            checkMutable();
            final Node<V> node = findValue(key);
            final int result = (node == null || node.nullKey ? 0 : node.key);
            if (node != null) delete(node);
            return result;
        }

        @Override
        public void putAll(final Map<? extends V, ? extends Integer> source) {
            checkMutable();
            for (final Map.Entry<? extends V, ? extends Integer> entry : source.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public void forEach(final BiConsumer<? super V, ? super Integer> consumer) {
            Objects.requireNonNull(consumer);
            for (Node<V> node = first; node != null; node = node.next)
                consumer.accept(boxedValue(node), boxedKey(node));
        }

        @Override
        public void replaceAll(
            final BiFunction<? super V, ? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Node<V> original = first;
            clear();
            for (Node<V> node = original; node != null; node = node.next)
                put(boxedValue(node), function.apply(boxedValue(node), boxedKey(node)));
        }

        @Override
        public Integer putIfAbsent(final V key, final Integer value) {
            checkMutable();
            final Integer old = get(key);
            return old == null ? put(key, value) : old;
        }

        @Override
        public boolean remove(final Object key, final Object value) {
            checkMutable();
            if (!containsKey(key) || !Objects.equals(get(key), value)) return false;
            remove(key);
            return true;
        }

        @Override
        public boolean replace(final V key, final Integer oldValue, final Integer newValue) {
            checkMutable();
            if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
            put(key, newValue);
            return true;
        }

        @Override
        public Integer replace(final V key, final Integer value) {
            checkMutable();
            return containsKey(key) ? put(key, value) : null;
        }

        @Override
        public Integer computeIfAbsent(
            final V key, @NotNull Function<? super V, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            Integer value = get(key);
            if (value == null && (value = function.apply(key)) != null) put(key, value);
            return value;
        }

        @Override
        public Integer computeIfPresent(
            final V key, @NotNull BiFunction<? super V, ? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Integer old = get(key);
            if (old == null) return null;
            final Integer value = function.apply(key, old);
            if (value == null) remove(key);
            else put(key, value);
            return value;
        }

        @Override
        public Integer compute(
            final V key, @NotNull BiFunction<? super V, ? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Integer value = function.apply(key, get(key));
            if (value == null) {
                if (containsKey(key)) remove(key);
            } else put(key, value);
            return value;
        }

        @Override
        public Integer merge(
            final V key,
            @NotNull Integer value,
            @NotNull BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            Objects.requireNonNull(value);
            final Integer old = get(key);
            final Integer result = old == null ? value : function.apply(old, value);
            if (result == null) remove(key);
            else put(key, result);
            return result;
        }

        @Override
        public void forEachObjectInt(final ObjIntConsumer<? super V> consumer) {
            Objects.requireNonNull(consumer);
            for (Node<V> node = first; node != null; node = node.next)
                consumer.accept(boxedValue(node), requiredKey(node));
        }

        @Override
        @NotNull
        public ObjectSet<V> keySet() {
            return Int2ObjectHashBiMap.this.values();
        }

        @Override
        @NotNull
        public IntSet values() {
            return Int2ObjectHashBiMap.this.keySet();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        @NotNull
        public ObjectSet<Map.Entry<V, Integer>> entrySet() {
            return (ObjectSet) object2IntEntrySet();
        }

        @Override
        public ObjectSet<Object2IntMap.Entry<V>> object2IntEntrySet() {
            if (inverseEntries == null) {
                final ObjectSet<Object2IntMap.Entry<V>> view =
                    new AbstractObjectSet<>() {
                        @Override
                        public int size() {
                            return size;
                        }

                        @Override
                        public void clear() {
                            Int2ObjectHashBiMap.this.clear();
                        }

                        @Override
                        public boolean contains(final Object value) {
                            if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                            final Node<V> node = findValue(entry.getKey());
                            return node != null
                                && Objects.equals(boxedKey(node), entry.getValue());
                        }

                        @Override
                        public boolean remove(final Object value) {
                            checkMutable();
                            if (!contains(value)) return false;
                            Inverse.this.remove(((Map.Entry<?, ?>) value).getKey());
                            return true;
                        }

                        @Override
                        @NotNull
                        public ObjectIterator<Object2IntMap.Entry<V>> iterator() {
                            return new Cursor<Object2IntMap.Entry<V>>() {
                                @Override
                                Object2IntMap.Entry<V> output(final Node<V> node) {
                                    return new InverseEntry(node, this);
                                }
                            };
                        }
                    };
                inverseEntries = frozen ? ObjectSets.unmodifiable(view) : view;
            }
            return inverseEntries;
        }
    }

    private abstract class Cursor<T> implements ObjectIterator<T> {
        private Node<V> nextNode = first, toRemove;
        private int remaining = size;
        private long expected = modifications;

        private void check() {
            if (expected != modifications) throw new ConcurrentModificationException();
        }

        @Override
        public boolean hasNext() {
            check();
            return nextNode != null && remaining > 0;
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            final Node<V> node = nextNode;
            nextNode = node.next;
            toRemove = node;
            remaining--;
            return output(node);
        }

        @Override
        public void remove() {
            checkMutable();
            check();
            if (toRemove == null) throw new IllegalStateException();
            delete(toRemove);
            expected = modifications;
            toRemove = null;
        }

        abstract T output(Node<V> node);
    }

    /**
     * Primitive iteration reads the payload array; boxed operations retain nullable BiMap compatibility.
     */
    private final class IntKeySet extends AbstractIntSet {
        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Int2ObjectHashBiMap.this.clear();
        }

        @Override
        public boolean contains(final int value) {
            return findKey(value, false) != null;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean contains(final Object value) {
            return findKeyBoxed(value) != null;
        }

        @Override
        public boolean remove(final int value) {
            checkMutable();
            final Node<V> node = findKey(value, false);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final Object value) {
            checkMutable();
            final Node<V> node = findKeyBoxed(value);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @NotNull
        public IntIterator iterator() {
            return new IntIterator() {
                private Node<V> nextNode = first, toRemove;
                private long expected = modifications;

                private void check() {
                    if (expected != modifications) throw new ConcurrentModificationException();
                }

                @Override
                public boolean hasNext() {
                    check();
                    return nextNode != null;
                }

                private Node<V> advance() {
                    if (!hasNext()) throw new NoSuchElementException();
                    final Node<V> node = nextNode;
                    nextNode = node.next;
                    toRemove = node;
                    return node;
                }

                @Override
                public int nextInt() {
                    return requiredKey(advance());
                }

                @Override
                @SuppressWarnings("deprecation")
                public Integer next() {
                    return boxedKey(advance());
                }

                @Override
                public void remove() {
                    checkMutable();
                    check();
                    if (toRemove == null) throw new IllegalStateException();
                    delete(toRemove);
                    expected = modifications;
                    toRemove = null;
                }
            };
        }

        @Override
        public boolean containsAll(final Collection<?> values) {
            for (final Object value : values) if (!contains(value)) return false;
            return true;
        }

        @Override
        public boolean containsAll(final IntCollection values) {
            return containsAll((Collection<?>) values);
        }

        @Override
        public boolean removeAll(final @NotNull Collection<?> values) {
            checkMutable();
            boolean changed = false;
            final Iterator<Integer> iterator = iterator();
            while (iterator.hasNext())
                if (values.contains(iterator.next())) {
                    iterator.remove();
                    changed = true;
                }
            return changed;
        }

        @Override
        public boolean retainAll(final @NotNull Collection<?> values) {
            checkMutable();
            boolean changed = false;
            final Iterator<Integer> iterator = iterator();
            while (iterator.hasNext())
                if (!values.contains(iterator.next())) {
                    iterator.remove();
                    changed = true;
                }
            return changed;
        }

        @Override
        public boolean removeAll(final IntCollection values) {
            return removeAll((Collection<?>) values);
        }

        @Override
        public boolean retainAll(final IntCollection values) {
            return retainAll((Collection<?>) values);
        }

        @Override
        public Object @NotNull [] toArray() {
            final Object[] array = new Object[size];
            int index = 0;
            for (final Integer value : this) array[index++] = value;
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T @NotNull [] toArray(final T[] supplied) {
            final T[] result = supplied.length >= size ? supplied : Arrays.copyOf(supplied, size);
            int index = 0;
            for (final Integer value : this) result[index++] = (T) value;
            if (result.length > index) result[index] = null;
            return result;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (Node<V> node = first; node != null; node = node.next)
                if (!node.nullKey) hash += node.key;
            return hash;
        }

        @Override
        public boolean equals(final Object other) {
            return other == this
                || other instanceof Set<?> set && set.size() == size && containsAll(set);
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder("[");
            for (final Integer value : this) {
                if (result.length() > 1) result.append(", ");
                result.append(value);
            }
            return result.append(']').toString();
        }
    }

    private final class ForwardEntry implements Int2ObjectMap.Entry<V> {
        private final Cursor<?> cursor;
        private Node<V> node;

        ForwardEntry(final Node<V> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getKey() {
            return boxedKey(node);
        }

        @Override
        public V getValue() {
            return boxedValue(node);
        }

        @Override
        public int getIntKey() {
            return requiredKey(node);
        }

        @Override
        public V setValue(final V value) {
            checkMutable();
            final V previousValue = node.value;
            final int hash = smear(Objects.hashCode(value));
            if (node.valueHash == hash && Objects.equals(value, previousValue)) return value;
            if (seekValue(value, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<V> previous = node;
            final Node<V> replacement =
                new Node<>(node.key, node.nullKey, node.keyHash, value, hash);
            replaceNodeForKey(previous, replacement);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacement;
            node = replacement;
            return previousValue;
        }

        @Override
        public int hashCode() {
            return (node.nullKey ? 0 : node.key) ^ Objects.hashCode(node.value);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                && Objects.equals(getKey(), entry.getKey())
                && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private final class InverseEntry implements Object2IntMap.Entry<V> {
        private final Cursor<?> cursor;
        private Node<V> node;

        InverseEntry(final Node<V> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        public V getKey() {
            return boxedValue(node);
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getValue() {
            return boxedKey(node);
        }

        @Override
        public int getIntValue() {
            return requiredKey(node);
        }

        private Node<V> replace(final int replacement, final boolean isNull) {
            checkMutable();
            final int hash = smear(isNull ? 0 : replacement);
            if (node.keyHash == hash
                && (node.nullKey == isNull && (isNull || node.key == replacement))) return node;
            if (seekKey(replacement, isNull, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<V> previous = node;
            final Node<V> replacementNode =
                new Node<>(replacement, isNull, hash, node.value, node.valueHash);
            delete(previous);
            append(replacementNode);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacementNode;
            node = replacementNode;
            return previous;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer setValue(final Integer value) {
            final Node<V> previous = replace(value == null ? 0 : value, value == null);
            if (previous == node) return value;
            return boxedKey(previous);
        }

        @Override
        public int setValue(final int value) {
            final Node<V> previous = replace(value, false);
            return previous.nullKey ? 0 : previous.key;
        }

        @Override
        public int hashCode() {
            return (node.nullKey ? 0 : node.key) ^ Objects.hashCode(node.value);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                && Objects.equals(getKey(), entry.getKey())
                && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
