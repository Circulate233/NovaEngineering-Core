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

import com.google.common.collect.BiMap;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Primitive specialization of Guava 33.6.0 HashBiMap. Bucket chains, cached hashes,
 * insertion order and forward/inverse replacement rules follow the original implementation.
 */
public final class ObjectIntPairHashBiMap<K, V> extends AbstractMap<ObjectIntPair<K>, V>
    implements BiMap<ObjectIntPair<K>, V>, Serializable {
    private final Inverse inverse = new Inverse();
    private transient Node<K, V>[] keyTable, valueTable;
    private transient Node<K, V> first, last;
    private transient int size, mask;
    private transient long modifications;
    private boolean frozen;
    private transient ObjectSet<ObjectIntPair<K>> keyView;
    private transient ObjectSet<V> valueView;
    private transient ObjectSet<Map.Entry<ObjectIntPair<K>, V>> entries;

    public ObjectIntPairHashBiMap() {
        this(16);
    }

    public ObjectIntPairHashBiMap(final int expectedSize) {
        init(expectedSize);
    }

    private static int smear(final int hash) {
        return Integer.rotateLeft(hash * 0xcc9e2d51, 15) * 0x1b873593;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ObjectIntPairHashBiMap<K, V> copyOf(
        final Map<? extends ObjectIntPair<K>, ? extends V> source) {
        final ObjectIntPairHashBiMap<K, V> result = new ObjectIntPairHashBiMap<>(source.size());
        if (source instanceof ObjectIntPairHashBiMap<?, ?> nativeMap) {
            final ObjectIntPairHashBiMap<K, V> typed = (ObjectIntPairHashBiMap<K, V>) nativeMap;
            for (Node<K, V> node = typed.first; node != null; node = node.next)
                result.append(new Node<>(node.key, node.keyHash, node.value, node.valueHash));
        } else result.putAll(source);
        return result;
    }

    public static <K, V> ObjectIntPairHashBiMap<K, V> immutableCopyOf(
        final Map<? extends ObjectIntPair<K>, ? extends V> source) {
        final ObjectIntPairHashBiMap<K, V> result = copyOf(source);
        for (Node<K, V> node = result.first; node != null; node = node.next) {
            if (node.key == null || node.value == null)
                throw new NullPointerException("null mapping");
        }
        result.frozen = true;
        return result;
    }

    public static <K, V> ObjectIntPairHashBiMap<K, V> forceCopyOf(
        final Map<? extends ObjectIntPair<K>, ? extends V> source) {
        final ObjectIntPairHashBiMap<K, V> result = new ObjectIntPairHashBiMap<>(source.size());
        for (final Map.Entry<? extends ObjectIntPair<K>, ? extends V> entry : source.entrySet()) {
            final ObjectIntPair<K> key = entry.getKey();
            if (key == null) result.forcePut(null, entry.getValue());
            else result.forcePut(key.left(), key.rightInt(), entry.getValue());
        }
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

    private Node<K, V> findKey(final Object value) {
        return seekKey(value, smear(Objects.hashCode(value)));
    }

    private Node<K, V> seekKey(final Object value, final int hash) {
        for (Node<K, V> node = keyTable[hash & mask]; node != null; node = node.nextKey)
            if (node.keyHash == hash && Objects.equals(value, node.key)) return node;
        return null;
    }

    private ObjectIntPair<K> boxedKey(final Node<K, V> node) {
        return node.key;
    }

    private Node<K, V> findValue(final Object value) {
        return seekValue(value, smear(Objects.hashCode(value)));
    }

    private Node<K, V> seekValue(final Object value, final int hash) {
        for (Node<K, V> node = valueTable[hash & mask]; node != null; node = node.nextValue)
            if (node.valueHash == hash && Objects.equals(value, node.value)) return node;
        return null;
    }

    private V boxedValue(final Node<K, V> node) {
        return node.value;
    }

    private void delete(final Node<K, V> node) {
        int bucket = node.keyHash & mask;
        Node<K, V> previous = null;
        for (Node<K, V> current = keyTable[bucket]; ; current = current.nextKey) {
            if (current == node) {
                if (previous == null) keyTable[bucket] = node.nextKey;
                else previous.nextKey = node.nextKey;
                break;
            }
            previous = current;
        }
        bucket = node.valueHash & mask;
        previous = null;
        for (Node<K, V> current = valueTable[bucket]; ; current = current.nextValue) {
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

    private void index(final Node<K, V> node) {
        final int keyBucket = node.keyHash & mask, valueBucket = node.valueHash & mask;
        node.nextKey = keyTable[keyBucket];
        keyTable[keyBucket] = node;
        node.nextValue = valueTable[valueBucket];
        valueTable[valueBucket] = node;
        size++;
        modifications++;
    }

    private void splice(final Node<K, V> node, final Node<K, V> before, final Node<K, V> after) {
        index(node);
        node.prev = before;
        node.next = after;
        if (before == null) first = node;
        else before.next = node;
        if (after == null) last = node;
        else after.prev = node;
    }

    private void append(final Node<K, V> node) {
        splice(node, last, null);
    }

    private void replaceNodeForKey(final Node<K, V> oldNode, final Node<K, V> newNode) {
        final Node<K, V> before = oldNode.prev, after = oldNode.next;
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
        for (Node<K, V> node = first; node != null; node = node.next) index(node);
        modifications++;
    }

    private Node<K, V> putForward(final ObjectIntPair<K> key, final V value, final boolean force) {
        checkMutable();
        final int keyHash = smear(Objects.hashCode(key)),
            valueHash = smear(Objects.hashCode(value));
        final Node<K, V> oldKey = seekKey(key, keyHash);
        if (oldKey != null
            && oldKey.valueHash == valueHash
            && (Objects.equals(value, oldKey.value))) return oldKey;
        final Node<K, V> oldValue = seekValue(value, valueHash);
        if (oldValue != null) {
            if (!force) throw new IllegalArgumentException("value already present");
            delete(oldValue);
        }
        final Node<K, V> node = new Node<>(key, keyHash, value, valueHash);
        if (oldKey != null) {
            replaceNodeForKey(oldKey, node);
            return oldKey;
        }
        append(node);
        rehashIfNecessary();
        return null;
    }

    private Node<K, V> putInverse(final ObjectIntPair<K> key, final V value, final boolean force) {
        checkMutable();
        final int valueHash = smear(Objects.hashCode(value)),
            keyHash = smear(Objects.hashCode(key));
        final Node<K, V> oldValue = seekValue(value, valueHash);
        final Node<K, V> oldKey = seekKey(key, keyHash);
        if (oldValue != null && oldValue.keyHash == keyHash && (Objects.equals(key, oldValue.key)))
            return oldValue;
        if (oldKey != null && !force) throw new IllegalArgumentException("key already present");
        if (oldValue != null) delete(oldValue);
        final Node<K, V> node = new Node<>(key, keyHash, value, valueHash);
        if (oldKey != null) replaceNodeForKey(oldKey, node);
        else append(node);
        rehashIfNecessary();
        return oldValue;
    }

    @Override
    public int size() {
        return size;
    }

    public long modificationVersion() {
        return modifications;
    }

    @Override
    public BiMap<V, ObjectIntPair<K>> inverse() {
        return inverse;
    }

    @Override
    public boolean containsKey(final Object key) {
        return seekKey(key, smear(Objects.hashCode(key))) != null;
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
        final Node<K, V> node = findKey(key);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public V getOrDefault(final Object key, final V fallback) {
        final Node<K, V> node = findKey(key);
        return node == null ? fallback : boxedValue(node);
    }

    @Override
    public V put(final ObjectIntPair<K> key, final V value) {
        return putBoxed(key, value, false);
    }

    @Override
    public V forcePut(final ObjectIntPair<K> key, final V value) {
        return putBoxed(key, value, true);
    }

    private V putBoxed(final ObjectIntPair<K> key, final V value, final boolean force) {
        final Node<K, V> previous = putForward(key, value, force);
        if (previous == null) return null;
        if (Objects.equals(value, previous.value)) return value;
        return boxedValue(previous);
    }

    @Override
    public V remove(final Object key) {
        checkMutable();
        final Node<K, V> node = findKey(key);
        if (node == null) return null;
        final V value = boxedValue(node);
        delete(node);
        return value;
    }

    @Override
    public void putAll(final Map<? extends ObjectIntPair<K>, ? extends V> source) {
        checkMutable();
        if (source instanceof ObjectIntPairHashBiMap<?, ?> nativeMap) {
            @SuppressWarnings("unchecked") final ObjectIntPairHashBiMap<K, V> typed = (ObjectIntPairHashBiMap<K, V>) nativeMap;
            for (Node<K, V> node = typed.first; node != null; node = node.next)
                putForward(node.key, node.value, false);
            return;
        }

        for (final Map.Entry<? extends ObjectIntPair<K>, ? extends V> entry : source.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public void forEach(final BiConsumer<? super ObjectIntPair<K>, ? super V> consumer) {
        Objects.requireNonNull(consumer);
        for (Node<K, V> node = first; node != null; node = node.next)
            consumer.accept(boxedKey(node), boxedValue(node));
    }

    @Override
    public void replaceAll(
        final BiFunction<? super ObjectIntPair<K>, ? super V, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final Node<K, V> original = first;
        clear();
        for (Node<K, V> node = original; node != null; node = node.next)
            put(boxedKey(node), function.apply(boxedKey(node), boxedValue(node)));
    }

    @Override
    public V putIfAbsent(final ObjectIntPair<K> key, final V value) {
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
    public boolean replace(final ObjectIntPair<K> key, final V oldValue, final V newValue) {
        checkMutable();
        if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    public V replace(final ObjectIntPair<K> key, final V value) {
        checkMutable();
        return containsKey(key) ? put(key, value) : null;
    }

    @Override
    public V computeIfAbsent(
        final ObjectIntPair<K> key, Function<? super ObjectIntPair<K>, ? extends V> function) {
        checkMutable();
        Objects.requireNonNull(function);
        V value = get(key);
        if (value == null && (value = function.apply(key)) != null) put(key, value);
        return value;
    }

    @Override
    public V computeIfPresent(
        final ObjectIntPair<K> key,
        BiFunction<? super ObjectIntPair<K>, ? super V, ? extends V> function) {
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
        final ObjectIntPair<K> key,
        BiFunction<? super ObjectIntPair<K>, ? super V, ? extends V> function) {
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
        final ObjectIntPair<K> key,
        V value,
        BiFunction<? super V, ? super V, ? extends V> function) {
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
    public ObjectSet<ObjectIntPair<K>> keySet() {
        if (keyView == null) {
            final ObjectSet<ObjectIntPair<K>> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public boolean contains(final Object value) {
                        return seekKey(value, smear(Objects.hashCode(value))) != null;
                    }

                    @Override
                    public void clear() {
                        ObjectIntPairHashBiMap.this.clear();
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        final Node<K, V> node = findKey(value);
                        if (node == null) return false;
                        delete(node);
                        return true;
                    }

                    @Override
                    public ObjectIterator<ObjectIntPair<K>> iterator() {
                        return new Cursor<>() {
                            @Override
                            ObjectIntPair<K> output(final Node<K, V> node) {
                                return boxedKey(node);
                            }
                        };
                    }
                };
            keyView = frozen ? ObjectSets.unmodifiable(view) : view;
        }
        return keyView;
    }

    @Override
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
                        ObjectIntPairHashBiMap.this.clear();
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        final Node<K, V> node = findValue(value);
                        if (node == null) return false;
                        delete(node);
                        return true;
                    }

                    @Override
                    public ObjectIterator<V> iterator() {
                        return new Cursor<>() {
                            @Override
                            V output(final Node<K, V> node) {
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
    public ObjectSet<Map.Entry<ObjectIntPair<K>, V>> entrySet() {
        if (entries == null) {
            final ObjectSet<Map.Entry<ObjectIntPair<K>, V>> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public void clear() {
                        ObjectIntPairHashBiMap.this.clear();
                    }

                    @Override
                    public boolean contains(final Object value) {
                        if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                        final Node<K, V> node = findKey(entry.getKey());
                        return node != null
                            && Objects.equals(boxedValue(node), entry.getValue());
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        if (!contains(value)) return false;
                        ObjectIntPairHashBiMap.this.remove(((Map.Entry<?, ?>) value).getKey());
                        return true;
                    }

                    @Override
                    public ObjectIterator<Map.Entry<ObjectIntPair<K>, V>> iterator() {
                        return new Cursor<>() {
                            @Override
                            Map.Entry<ObjectIntPair<K>, V> output(final Node<K, V> node) {
                                return new ForwardEntry(node, this);
                            }
                        };
                    }
                };
            entries = frozen ? ObjectSets.unmodifiable(view) : view;
        }
        return entries;
    }

    private Node<K, V> findParts(final K left, final int right) {
        final int hash = smear(Objects.hashCode(left) * 19 + right);
        for (Node<K, V> node = keyTable[hash & mask]; node != null; node = node.nextKey)
            if (node.keyHash == hash
                && node.key != null
                && node.key.rightInt() == right
                && Objects.equals(left, node.key.left())) return node;
        return null;
    }

    public V get(final K left, final int right) {
        final Node<K, V> node = findParts(left, right);
        return node == null ? null : boxedValue(node);
    }

    public boolean containsKey(final K left, final int right) {
        return findParts(left, right) != null;
    }

    public V remove(final K left, final int right) {
        checkMutable();
        final Node<K, V> node = findParts(left, right);
        if (node == null) return null;
        final V value = boxedValue(node);
        delete(node);
        return value;
    }

    public V put(final K left, final int right, final V value) {
        final Node<K, V> node = findParts(left, right);
        if (node != null && Objects.equals(boxedValue(node), value)) {
            checkMutable();
            return value;
        }
        return put(node == null ? ObjectIntImmutablePair.of(left, right) : node.key, value);
    }

    public V forcePut(final K left, final int right, final V value) {
        final Node<K, V> node = findParts(left, right);
        if (node != null && Objects.equals(boxedValue(node), value)) {
            checkMutable();
            return value;
        }
        return forcePut(node == null ? ObjectIntImmutablePair.of(left, right) : node.key, value);
    }

    public int getIntKeyForValue(final Object value) {
        final Node<K, V> node = findValue(value);
        if (node == null) throw new NullPointerException("null integer mapping");
        return boxedKey(node).rightInt();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(size);
        for (Node<K, V> node = first; node != null; node = node.next) {
            stream.writeObject(node.key);
            stream.writeObject(node.value);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        final int count = stream.readInt();
        if (count < 0) throw new InvalidObjectException("negative size");
        init(count);
        final boolean wasFrozen = frozen;
        frozen = false;
        for (int i = 0; i < count; i++) {
            final ObjectIntPair<K> key = (ObjectIntPair<K>) stream.readObject();
            final V value = (V) stream.readObject();
            putForward(key, value, false);
        }
        frozen = wasFrozen;
    }

    private static final class Node<K, V> {
        final int keyHash, valueHash;
        final ObjectIntPair<K> key;
        final V value;
        Node<K, V> nextKey, nextValue, prev, next;

        Node(final ObjectIntPair<K> key, final int keyHash, final V value, final int valueHash) {
            this.key = key;
            this.value = value;
            this.keyHash = keyHash;
            this.valueHash = valueHash;
        }
    }

    private final class Inverse extends AbstractMap<V, ObjectIntPair<K>>
        implements BiMap<V, ObjectIntPair<K>>, Serializable {
        private transient ObjectSet<Map.Entry<V, ObjectIntPair<K>>> inverseEntries;

        @Override
        public int size() {
            return size;
        }

        public long modificationVersion() {
            return modifications;
        }

        @Override
        public BiMap<ObjectIntPair<K>, V> inverse() {
            return ObjectIntPairHashBiMap.this;
        }

        @Override
        public void clear() {
            ObjectIntPairHashBiMap.this.clear();
        }

        @Override
        public boolean containsKey(final Object key) {
            return seekValue(key, smear(Objects.hashCode(key))) != null;
        }

        @Override
        public boolean containsValue(final Object value) {
            return seekKey(value, smear(Objects.hashCode(value))) != null;
        }

        @Override
        public ObjectIntPair<K> get(final Object key) {
            final Node<K, V> node = findValue(key);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public ObjectIntPair<K> getOrDefault(final Object key, final ObjectIntPair<K> fallback) {
            final Node<K, V> node = findValue(key);
            return node == null ? fallback : boxedKey(node);
        }

        @Override
        public ObjectIntPair<K> put(final V key, final ObjectIntPair<K> value) {
            return putBoxed(key, value, false);
        }

        @Override
        public ObjectIntPair<K> forcePut(final V key, final ObjectIntPair<K> value) {
            return putBoxed(key, value, true);
        }

        private ObjectIntPair<K> putBoxed(
            final V key, final ObjectIntPair<K> value, final boolean force) {
            final Node<K, V> previous = putInverse(value, key, force);
            if (previous == null) return null;
            if (Objects.equals(value, previous.key)) return value;
            return boxedKey(previous);
        }

        @Override
        public ObjectIntPair<K> remove(final Object key) {
            checkMutable();
            final Node<K, V> node = findValue(key);
            if (node == null) return null;
            final ObjectIntPair<K> value = boxedKey(node);
            delete(node);
            return value;
        }

        @Override
        public void putAll(final Map<? extends V, ? extends ObjectIntPair<K>> source) {
            checkMutable();
            for (final Map.Entry<? extends V, ? extends ObjectIntPair<K>> entry : source.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public void forEach(final BiConsumer<? super V, ? super ObjectIntPair<K>> consumer) {
            Objects.requireNonNull(consumer);
            for (Node<K, V> node = first; node != null; node = node.next)
                consumer.accept(boxedValue(node), boxedKey(node));
        }

        @Override
        public void replaceAll(
            final BiFunction<? super V, ? super ObjectIntPair<K>, ? extends ObjectIntPair<K>>
                function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Node<K, V> original = first;
            clear();
            for (Node<K, V> node = original; node != null; node = node.next)
                put(boxedValue(node), function.apply(boxedValue(node), boxedKey(node)));
        }

        @Override
        public ObjectIntPair<K> putIfAbsent(final V key, final ObjectIntPair<K> value) {
            checkMutable();
            final ObjectIntPair<K> old = get(key);
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
        public boolean replace(
            final V key, final ObjectIntPair<K> oldValue, final ObjectIntPair<K> newValue) {
            checkMutable();
            if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
            put(key, newValue);
            return true;
        }

        @Override
        public ObjectIntPair<K> replace(final V key, final ObjectIntPair<K> value) {
            checkMutable();
            return containsKey(key) ? put(key, value) : null;
        }

        @Override
        public ObjectIntPair<K> computeIfAbsent(
            final V key, Function<? super V, ? extends ObjectIntPair<K>> function) {
            checkMutable();
            Objects.requireNonNull(function);
            ObjectIntPair<K> value = get(key);
            if (value == null && (value = function.apply(key)) != null) put(key, value);
            return value;
        }

        @Override
        public ObjectIntPair<K> computeIfPresent(
            final V key,
            BiFunction<? super V, ? super ObjectIntPair<K>, ? extends ObjectIntPair<K>>
                function) {
            checkMutable();
            Objects.requireNonNull(function);
            final ObjectIntPair<K> old = get(key);
            if (old == null) return null;
            final ObjectIntPair<K> value = function.apply(key, old);
            if (value == null) remove(key);
            else put(key, value);
            return value;
        }

        @Override
        public ObjectIntPair<K> compute(
            final V key,
            BiFunction<? super V, ? super ObjectIntPair<K>, ? extends ObjectIntPair<K>>
                function) {
            checkMutable();
            Objects.requireNonNull(function);
            final ObjectIntPair<K> value = function.apply(key, get(key));
            if (value == null) {
                if (containsKey(key)) remove(key);
            } else put(key, value);
            return value;
        }

        @Override
        public ObjectIntPair<K> merge(
            final V key,
            ObjectIntPair<K> value,
            BiFunction<
                ? super ObjectIntPair<K>,
                ? super ObjectIntPair<K>,
                ? extends ObjectIntPair<K>>
                function) {
            checkMutable();
            Objects.requireNonNull(function);
            Objects.requireNonNull(value);
            final ObjectIntPair<K> old = get(key);
            final ObjectIntPair<K> result = old == null ? value : function.apply(old, value);
            if (result == null) remove(key);
            else put(key, result);
            return result;
        }

        @Override
        public ObjectSet<V> keySet() {
            return ObjectIntPairHashBiMap.this.values();
        }

        @Override
        public ObjectSet<ObjectIntPair<K>> values() {
            return ObjectIntPairHashBiMap.this.keySet();
        }

        @Override
        public ObjectSet<Map.Entry<V, ObjectIntPair<K>>> entrySet() {
            if (inverseEntries == null) {
                final ObjectSet<Map.Entry<V, ObjectIntPair<K>>> view =
                    new AbstractObjectSet<>() {
                        @Override
                        public int size() {
                            return size;
                        }

                        @Override
                        public void clear() {
                            ObjectIntPairHashBiMap.this.clear();
                        }

                        @Override
                        public boolean contains(final Object value) {
                            if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                            final Node<K, V> node = findValue(entry.getKey());
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
                        public ObjectIterator<Map.Entry<V, ObjectIntPair<K>>> iterator() {
                            return new Cursor<>() {
                                @Override
                                Map.Entry<V, ObjectIntPair<K>> output(final Node<K, V> node) {
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
        private Node<K, V> nextNode = first, toRemove;
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
            final Node<K, V> node = nextNode;
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

        abstract T output(Node<K, V> node);
    }

    private final class ForwardEntry implements Map.Entry<ObjectIntPair<K>, V> {
        private final Cursor<?> cursor;
        private Node<K, V> node;

        ForwardEntry(final Node<K, V> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        public ObjectIntPair<K> getKey() {
            return boxedKey(node);
        }

        @Override
        public V getValue() {
            return boxedValue(node);
        }

        @Override
        public V setValue(final V value) {
            checkMutable();
            final V previousValue = node.value;
            final int hash = smear(Objects.hashCode(value));
            if (node.valueHash == hash && Objects.equals(value, previousValue)) return value;
            if (seekValue(value, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<K, V> previous = node;
            final Node<K, V> replacement = new Node<>(node.key, node.keyHash, value, hash);
            replaceNodeForKey(previous, replacement);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacement;
            node = replacement;
            return previousValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
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

    private final class InverseEntry implements Map.Entry<V, ObjectIntPair<K>> {
        private final Cursor<?> cursor;
        private Node<K, V> node;

        InverseEntry(final Node<K, V> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        public V getKey() {
            return boxedValue(node);
        }

        @Override
        public ObjectIntPair<K> getValue() {
            return boxedKey(node);
        }

        @Override
        public ObjectIntPair<K> setValue(final ObjectIntPair<K> value) {
            checkMutable();
            final ObjectIntPair<K> previousValue = node.key;
            final int hash = smear(Objects.hashCode(value));
            if (node.keyHash == hash && Objects.equals(value, previousValue)) return value;
            if (seekKey(value, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<K, V> previous = node;
            final Node<K, V> replacement = new Node<>(value, hash, node.value, node.valueHash);
            delete(previous);
            append(replacement);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacement;
            node = replacement;
            return previousValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
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
