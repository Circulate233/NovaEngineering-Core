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
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.AbstractReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
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
 * Reference-key variant of Object2IntHashBiMap. Only object-key equality and hashing use
 * reference identity; bucket chains, insertion order, primitive IDs and replacement rules are unchanged.
 */
// Backed views intentionally retain their map; fastutil supplies the inherited Cloneable marker.
@SuppressWarnings({"ReturnOfInnerClass", "CloneableClassInSecureContext", "unused"})
public final class Reference2IntHashBiMap<K> extends AbstractMap<K, Integer>
    implements Reference2IntBiMap<K>, Serializable {
    private final Inverse inverse = new Inverse();
    private final Class<?> enumType;
    private transient Node<K>[] keyTable, valueTable;
    private transient Node<K> first, last;
    private transient int size, mask;
    private transient long modifications;
    private boolean frozen;
    private transient ReferenceSet<K> keyView;
    private transient IntSet valueView;
    private transient ObjectSet<Reference2IntMap.Entry<K>> entries;

    public Reference2IntHashBiMap() {
        this(16);
    }

    public Reference2IntHashBiMap(final int expectedSize) {
        this(expectedSize, null);
    }

    private Reference2IntHashBiMap(final int expectedSize, final Class<?> enumType) {
        this.enumType = enumType;
        init(expectedSize);
    }

    public static <E extends Enum<E>> Reference2IntHashBiMap<E> forEnum(final Class<E> type) {
        if (!Objects.requireNonNull(type).isEnum())
            throw new IllegalArgumentException("Not an enum type");
        return new Reference2IntHashBiMap<>(type.getEnumConstants().length, type);
    }

    private static int smear(final int hash) {
        return Integer.rotateLeft(hash * 0xcc9e2d51, 15) * 0x1b873593;
    }

    @SuppressWarnings("unchecked")
    public static <K> Reference2IntHashBiMap<K> copyOf(
        final Map<? extends K, ? extends Integer> source) {
        final Reference2IntHashBiMap<K> result = new Reference2IntHashBiMap<>(source.size());
        if (source instanceof Reference2IntHashBiMap<?> nativeMap) {
            final Reference2IntHashBiMap<K> typed = (Reference2IntHashBiMap<K>) nativeMap;
            for (Node<K> node = typed.first; node != null; node = node.next)
                result.append(
                    new Node<>(
                        node.key,
                        node.keyHash,
                        node.value,
                        node.nullValue,
                        node.valueHash));
        } else result.putAll(source);
        return result;
    }

    public static <K> Reference2IntHashBiMap<K> immutableCopyOf(
        final Map<? extends K, ? extends Integer> source) {
        final Reference2IntHashBiMap<K> result = copyOf(source);
        for (Node<K> node = result.first; node != null; node = node.next) {
            if (node.key == null || node.nullValue) throw new NullPointerException("null mapping");
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

    private Node<K> findKey(final Object value) {
        return seekKey(value, smear(System.identityHashCode(value)));
    }

    private Node<K> seekKey(final Object value, final int hash) {
        for (Node<K> node = keyTable[hash & mask]; node != null; node = node.nextKey)
            if (node.keyHash == hash && value == node.key) return node;
        return null;
    }

    private K boxedKey(final Node<K> node) {
        return node.key;
    }

    private Node<K> findValue(final int number, final boolean isNull) {
        return seekValue(number, isNull, smear(isNull ? 0 : number));
    }

    private Node<K> findValueBoxed(final Object value) {
        return value == null
            ? findValue(0, true)
            : value instanceof Integer number ? findValue(number, false) : null;
    }

    private Node<K> seekValue(final int number, final boolean isNull, final int hash) {
        for (Node<K> node = valueTable[hash & mask]; node != null; node = node.nextValue)
            if (node.valueHash == hash
                && node.nullValue == isNull
                && (isNull || node.value == number)) return node;
        return null;
    }

    private Integer boxedValue(final Node<K> node) {
        return node.nullValue ? null : node.value;
    }

    private int requiredValue(final Node<K> node) {
        if (node == null || node.nullValue) throw new NullPointerException("null integer mapping");
        return node.value;
    }

    private void delete(final Node<K> node) {
        int bucket = node.keyHash & mask;
        Node<K> previous = null;
        for (Node<K> current = keyTable[bucket]; ; current = current.nextKey) {
            if (current == node) {
                if (previous == null) keyTable[bucket] = node.nextKey;
                else previous.nextKey = node.nextKey;
                break;
            }
            previous = current;
        }
        bucket = node.valueHash & mask;
        previous = null;
        for (Node<K> current = valueTable[bucket]; ; current = current.nextValue) {
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

    private void index(final Node<K> node) {
        final int keyBucket = node.keyHash & mask, valueBucket = node.valueHash & mask;
        node.nextKey = keyTable[keyBucket];
        keyTable[keyBucket] = node;
        node.nextValue = valueTable[valueBucket];
        valueTable[valueBucket] = node;
        size++;
        modifications++;
    }

    private void splice(final Node<K> node, final Node<K> before, final Node<K> after) {
        index(node);
        node.prev = before;
        node.next = after;
        if (before == null) first = node;
        else before.next = node;
        if (after == null) last = node;
        else after.prev = node;
    }

    private void append(final Node<K> node) {
        if (enumType != null) {
            Node<K> after = first;
            final int ordinal = ((Enum<?>) node.key).ordinal();
            while (after != null && ((Enum<?>) after.key).ordinal() < ordinal) after = after.next;
            splice(node, after == null ? last : after.prev, after);
            return;
        }
        splice(node, last, null);
    }

    private void replaceNodeForKey(final Node<K> oldNode, final Node<K> newNode) {
        final Node<K> before = oldNode.prev, after = oldNode.next;
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
        for (Node<K> node = first; node != null; node = node.next) index(node);
        modifications++;
    }

    private Node<K> putForward(
        final K key, final int value, final boolean valueNull, final boolean force) {
        checkMutable();
        if (enumType != null && !enumType.isInstance(Objects.requireNonNull(key)))
            throw new ClassCastException("Expected " + enumType.getName());
        final int keyHash = smear(System.identityHashCode(key)), valueHash = smear(valueNull ? 0 : value);
        final Node<K> oldKey = seekKey(key, keyHash);
        if (oldKey != null
            && oldKey.valueHash == valueHash
            && (oldKey.nullValue == valueNull && (valueNull || oldKey.value == value)))
            return oldKey;
        final Node<K> oldValue = seekValue(value, valueNull, valueHash);
        if (oldValue != null) {
            if (!force) throw new IllegalArgumentException("value already present");
            delete(oldValue);
        }
        final Node<K> node = new Node<>(key, keyHash, value, valueNull, valueHash);
        if (oldKey != null) {
            replaceNodeForKey(oldKey, node);
            return oldKey;
        }
        append(node);
        rehashIfNecessary();
        return null;
    }

    private Node<K> putInverse(
        final K key, final int value, final boolean valueNull, final boolean force) {
        checkMutable();
        if (enumType != null && !enumType.isInstance(Objects.requireNonNull(key)))
            throw new ClassCastException("Expected " + enumType.getName());
        final int valueHash = smear(valueNull ? 0 : value), keyHash = smear(System.identityHashCode(key));
        final Node<K> oldValue = seekValue(value, valueNull, valueHash);
        final Node<K> oldKey = seekKey(key, keyHash);
        if (oldValue != null && oldValue.keyHash == keyHash && (key == oldValue.key))
            return oldValue;
        if (oldKey != null && !force) throw new IllegalArgumentException("key already present");
        if (oldValue != null) delete(oldValue);
        final Node<K> node = new Node<>(key, keyHash, value, valueNull, valueHash);
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
    public Int2ReferenceBiMap<K> inverse() {
        return inverse;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) return true;
        if (!(other instanceof Map<?, ?> map) || map.size() != size) return false;
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Node<K> node = findKey(entry.getKey());
            if (node == null || !Objects.equals(boxedValue(node), entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public boolean containsKey(final Object key) {
        return seekKey(key, smear(System.identityHashCode(key))) != null;
    }

    @Override
    public boolean containsValue(final Object value) {
        return findValueBoxed(value) != null;
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
    public Integer get(final Object key) {
        final Node<K> node = findKey(key);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public Integer getOrDefault(final Object key, final Integer fallback) {
        final Node<K> node = findKey(key);
        return node == null ? fallback : boxedValue(node);
    }

    @Override
    public Integer put(final K key, final Integer value) {
        return putBoxed(key, value, false);
    }

    @Override
    public Integer forcePut(final K key, final Integer value) {
        return putBoxed(key, value, true);
    }

    private Integer putBoxed(final K key, final Integer value, final boolean force) {
        final Node<K> previous =
            putForward(key, value == null ? 0 : value, value == null, force);
        if (previous == null) return null;
        if (value == null
            ? previous.nullValue
            : !previous.nullValue && previous.value == value) return value;
        return boxedValue(previous);
    }

    @Override
    public Integer remove(final Object key) {
        checkMutable();
        final Node<K> node = findKey(key);
        if (node == null) return null;
        final Integer value = boxedValue(node);
        delete(node);
        return value;
    }

    @Override
    public boolean containsValue(final int value) {
        return findValue(value, false) != null;
    }

    @Override
    public int getInt(final Object key) {
        return getIntOrDefault(key, 0);
    }

    @Override
    public int getIntOrDefault(final Object key, final int fallback) {
        final Node<K> node = findKey(key);
        return node == null ? fallback : requiredValue(node);
    }

    @Override
    public int getIntRequired(final Object key) {
        return requiredValue(findKey(key));
    }

    @Override
    public int put(final K key, final int value) {
        return putPrimitive(key, value, false);
    }

    @Override
    public int forcePut(final K key, final int value) {
        return putPrimitive(key, value, true);
    }

    private int putPrimitive(final K key, final int value, final boolean force) {
        final Node<K> previous = putForward(key, value, false, force);
        return previous == null || previous.nullValue ? 0 : previous.value;
    }

    @Override
    public void putDiscard(final K key, final int value, final boolean force) {
        putForward(key, value, false, force);
    }

    @Override
    public int removeIntRequired(final Object key) {
        checkMutable();
        final Node<K> node = findKey(key);
        final boolean missing = node == null || node.nullValue;
        final int value = node == null ? 0 : node.value;
        if (node != null) delete(node);
        if (missing) throw new NullPointerException("null integer mapping");
        return value;
    }

    @Override
    public int removeInt(final Object key) {
        checkMutable();
        final Node<K> node = findKey(key);
        final int result = (node == null || node.nullValue ? 0 : node.value);
        if (node != null) delete(node);
        return result;
    }

    @Override
    public void putAll(final @NotNull Map<? extends K, ? extends Integer> source) {
        checkMutable();
        if (source instanceof Reference2IntHashBiMap<?> nativeMap) {
            @SuppressWarnings("unchecked") final Reference2IntHashBiMap<K> typed = (Reference2IntHashBiMap<K>) nativeMap;
            for (Node<K> node = typed.first; node != null; node = node.next)
                putForward(node.key, node.value, node.nullValue, false);
            return;
        }

        for (final Map.Entry<? extends K, ? extends Integer> entry : source.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super Integer> consumer) {
        Objects.requireNonNull(consumer);
        for (Node<K> node = first; node != null; node = node.next)
            consumer.accept(boxedKey(node), boxedValue(node));
    }

    @Override
    public void replaceAll(
        final BiFunction<? super K, ? super Integer, ? extends Integer> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final Node<K> original = first;
        clear();
        for (Node<K> node = original; node != null; node = node.next)
            put(boxedKey(node), function.apply(boxedKey(node), boxedValue(node)));
    }

    @Override
    public Integer putIfAbsent(final K key, final Integer value) {
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
    public boolean replace(final K key, final Integer oldValue, final Integer newValue) {
        checkMutable();
        if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    public Integer replace(final K key, final Integer value) {
        checkMutable();
        return containsKey(key) ? put(key, value) : null;
    }

    @Override
    public Integer computeIfAbsent(final K key, @NotNull Function<? super K, ? extends Integer> function) {
        checkMutable();
        Objects.requireNonNull(function);
        Integer value = get(key);
        if (value == null && (value = function.apply(key)) != null) put(key, value);
        return value;
    }

    @Override
    public Integer computeIfPresent(
        final K key, @NotNull BiFunction<? super K, ? super Integer, ? extends Integer> function) {
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
        final K key, @NotNull BiFunction<? super K, ? super Integer, ? extends Integer> function) {
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
        final K key,
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
    public void forEachObjectInt(final ObjIntConsumer<? super K> consumer) {
        Objects.requireNonNull(consumer);
        for (Node<K> node = first; node != null; node = node.next)
            consumer.accept(boxedKey(node), requiredValue(node));
    }

    @Override
    @NotNull
    public ReferenceSet<K> keySet() {
        if (keyView == null) {
            final ReferenceSet<K> view =
                new AbstractReferenceSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public boolean contains(final Object value) {
                        return seekKey(value, smear(System.identityHashCode(value))) != null;
                    }

                    @Override
                    public void clear() {
                        Reference2IntHashBiMap.this.clear();
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        final Node<K> node = findKey(value);
                        if (node == null) return false;
                        delete(node);
                        return true;
                    }

                    @Override
                    @NotNull
                    public ObjectIterator<K> iterator() {
                        return new Cursor<K>() {
                            @Override
                            K output(final Node<K> node) {
                                return boxedKey(node);
                            }
                        };
                    }
                };
            keyView = frozen ? ReferenceSets.unmodifiable(view) : view;
        }
        return keyView;
    }

    @Override
    @NotNull
    public IntSet values() {
        if (valueView == null) {
            final IntSet view = new IntValueSet();
            valueView = frozen ? IntSets.unmodifiable(view) : view;
        }
        return valueView;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    public ObjectSet<Map.Entry<K, Integer>> entrySet() {
        return (ObjectSet) reference2IntEntrySet();
    }

    @Override
    public ObjectSet<Reference2IntMap.Entry<K>> reference2IntEntrySet() {
        if (entries == null) {
            final ObjectSet<Reference2IntMap.Entry<K>> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public void clear() {
                        Reference2IntHashBiMap.this.clear();
                    }

                    @Override
                    public boolean contains(final Object value) {
                        if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                        final Node<K> node = findKey(entry.getKey());
                        return node != null
                            && Objects.equals(boxedValue(node), entry.getValue());
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        if (!contains(value)) return false;
                        Reference2IntHashBiMap.this.remove(((Map.Entry<?, ?>) value).getKey());
                        return true;
                    }

                    @Override
                    @NotNull
                    public ObjectIterator<Reference2IntMap.Entry<K>> iterator() {
                        return new Cursor<Reference2IntMap.Entry<K>>() {
                            @Override
                            Reference2IntMap.Entry<K> output(final Node<K> node) {
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
        for (Node<K> node = first; node != null; node = node.next) {
            stream.writeObject(node.key);
            stream.writeBoolean(node.nullValue);
            stream.writeInt(node.value);
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
            final K key = (K) stream.readObject();
            final boolean valueNull = stream.readBoolean();
            final int value = stream.readInt();
            putForward(key, value, valueNull, false);
        }
        frozen = wasFrozen;
    }

    private static final class Node<K> {
        final int keyHash, valueHash;
        final K key;
        final int value;
        final boolean nullValue;
        Node<K> nextKey, nextValue, prev, next;

        Node(
            final K key,
            final int keyHash,
            final int value,
            final boolean nullValue,
            final int valueHash) {
            this.key = key;
            this.value = value;
            this.keyHash = keyHash;
            this.valueHash = valueHash;
            this.nullValue = nullValue;
        }
    }

    private final class Inverse extends AbstractMap<Integer, K>
        implements Int2ReferenceBiMap<K>, Serializable {
        private transient ObjectSet<Int2ReferenceMap.Entry<K>> inverseEntries;

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
        public Reference2IntBiMap<K> inverse() {
            return Reference2IntHashBiMap.this;
        }

        @Override
        public void clear() {
            Reference2IntHashBiMap.this.clear();
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) return true;
            if (!(other instanceof Map<?, ?> map) || map.size() != size) return false;
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                final Node<K> node = findValueBoxed(entry.getKey());
                if (node == null || node.key != entry.getValue()) return false;
            }
            return true;
        }

        @Override
        public boolean containsKey(final Object key) {
            return findValueBoxed(key) != null;
        }

        @Override
        public boolean containsValue(final Object value) {
            return seekKey(value, smear(System.identityHashCode(value))) != null;
        }

        @Override
        public K get(final Object key) {
            final Node<K> node = findValueBoxed(key);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public K getOrDefault(final Object key, final K fallback) {
            final Node<K> node = findValueBoxed(key);
            return node == null ? fallback : boxedKey(node);
        }

        @Override
        public K put(final Integer key, final K value) {
            return putBoxed(key, value, false);
        }

        @Override
        public K forcePut(final Integer key, final K value) {
            return putBoxed(key, value, true);
        }

        private K putBoxed(final Integer key, final K value, final boolean force) {
            final Node<K> previous =
                putInverse(value, key == null ? 0 : key, key == null, force);
            if (previous == null) return null;
            if (value == previous.key) return value;
            return boxedKey(previous);
        }

        @Override
        public K remove(final Object key) {
            checkMutable();
            final Node<K> node = findValueBoxed(key);
            if (node == null) return null;
            final K value = boxedKey(node);
            delete(node);
            return value;
        }

        @Override
        public boolean containsKey(final int key) {
            return findValue(key, false) != null;
        }

        @Override
        public K get(final int key) {
            final Node<K> node = findValue(key, false);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public K getOrDefault(final int key, final K fallback) {
            final Node<K> node = findValue(key, false);
            return node == null ? fallback : boxedKey(node);
        }

        @Override
        public K put(final int key, final K value) {
            return putPrimitive(key, value, false);
        }

        @Override
        public K forcePut(final int key, final K value) {
            return putPrimitive(key, value, true);
        }

        private K putPrimitive(final int key, final K value, final boolean force) {
            final Node<K> previous = putInverse(value, key, false, force);
            return previous == null
                ? null
                : value == previous.key ? value : previous.key;
        }

        @Override
        public void putDiscard(final int key, final K value, final boolean force) {
            putInverse(value, key, false, force);
        }

        @Override
        public K remove(final int key) {
            checkMutable();
            final Node<K> node = findValue(key, false);
            final K result = (node == null ? null : boxedKey(node));
            if (node != null) delete(node);
            return result;
        }

        @Override
        public void putAll(final Map<? extends Integer, ? extends K> source) {
            checkMutable();
            for (final Map.Entry<? extends Integer, ? extends K> entry : source.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public void forEach(final BiConsumer<? super Integer, ? super K> consumer) {
            Objects.requireNonNull(consumer);
            for (Node<K> node = first; node != null; node = node.next)
                consumer.accept(boxedValue(node), boxedKey(node));
        }

        @Override
        public void replaceAll(final BiFunction<? super Integer, ? super K, ? extends K> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Node<K> original = first;
            clear();
            for (Node<K> node = original; node != null; node = node.next)
                put(boxedValue(node), function.apply(boxedValue(node), boxedKey(node)));
        }

        @Override
        public K putIfAbsent(final Integer key, final K value) {
            checkMutable();
            final K old = get(key);
            return old == null ? put(key, value) : old;
        }

        @Override
        public boolean remove(final Object key, final Object value) {
            checkMutable();
            if (!containsKey(key) || get(key) != value) return false;
            remove(key);
            return true;
        }

        @Override
        public boolean replace(final Integer key, final K oldValue, final K newValue) {
            checkMutable();
            if (!containsKey(key) || get(key) != oldValue) return false;
            put(key, newValue);
            return true;
        }

        @Override
        public K replace(final Integer key, final K value) {
            checkMutable();
            return containsKey(key) ? put(key, value) : null;
        }

        @Override
        public K computeIfAbsent(
            final Integer key, @NotNull Function<? super Integer, ? extends K> function) {
            checkMutable();
            Objects.requireNonNull(function);
            K value = get(key);
            if (value == null && (value = function.apply(key)) != null) put(key, value);
            return value;
        }

        @Override
        public K computeIfPresent(
            final Integer key, @NotNull BiFunction<? super Integer, ? super K, ? extends K> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final K old = get(key);
            if (old == null) return null;
            final K value = function.apply(key, old);
            if (value == null) remove(key);
            else put(key, value);
            return value;
        }

        @Override
        public K compute(
            final Integer key, @NotNull BiFunction<? super Integer, ? super K, ? extends K> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final K value = function.apply(key, get(key));
            if (value == null) {
                if (containsKey(key)) remove(key);
            } else put(key, value);
            return value;
        }

        @Override
        public K merge(
            final Integer key,
            @NotNull K value,
            @NotNull BiFunction<? super K, ? super K, ? extends K> function) {
            checkMutable();
            Objects.requireNonNull(function);
            Objects.requireNonNull(value);
            final K old = get(key);
            final K result = old == null ? value : function.apply(old, value);
            if (result == null) remove(key);
            else put(key, result);
            return result;
        }

        @Override
        public void forEachIntObject(final Int2ReferenceBiMap.IntObjectConsumer<? super K> consumer) {
            Objects.requireNonNull(consumer);
            for (Node<K> node = first; node != null; node = node.next)
                consumer.accept(requiredValue(node), boxedKey(node));
        }

        @Override
        @NotNull
        public IntSet keySet() {
            return Reference2IntHashBiMap.this.values();
        }

        @Override
        @NotNull
        public ReferenceSet<K> values() {
            return Reference2IntHashBiMap.this.keySet();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        @NotNull
        public ObjectSet<Map.Entry<Integer, K>> entrySet() {
            return (ObjectSet) int2ReferenceEntrySet();
        }

        @Override
        public ObjectSet<Int2ReferenceMap.Entry<K>> int2ReferenceEntrySet() {
            if (inverseEntries == null) {
                final ObjectSet<Int2ReferenceMap.Entry<K>> view =
                    new AbstractObjectSet<>() {
                        @Override
                        public int size() {
                            return size;
                        }

                        @Override
                        public void clear() {
                            Reference2IntHashBiMap.this.clear();
                        }

                        @Override
                        public boolean contains(final Object value) {
                            if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                            final Node<K> node = findValueBoxed(entry.getKey());
                            return node != null
                                && boxedKey(node) == entry.getValue();
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
                        public ObjectIterator<Int2ReferenceMap.Entry<K>> iterator() {
                            return new Cursor<Int2ReferenceMap.Entry<K>>() {
                                @Override
                                Int2ReferenceMap.Entry<K> output(final Node<K> node) {
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
        private Node<K> nextNode = first, toRemove;
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
            final Node<K> node = nextNode;
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

        abstract T output(Node<K> node);
    }

    /**
     * Primitive iteration reads the payload array; boxed operations retain nullable BiMap compatibility.
     */
    private final class IntValueSet extends AbstractIntSet {
        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            Reference2IntHashBiMap.this.clear();
        }

        @Override
        public boolean contains(final int value) {
            return findValue(value, false) != null;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean contains(final Object value) {
            return findValueBoxed(value) != null;
        }

        @Override
        public boolean remove(final int value) {
            checkMutable();
            final Node<K> node = findValue(value, false);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final Object value) {
            checkMutable();
            final Node<K> node = findValueBoxed(value);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @NotNull
        public IntIterator iterator() {
            return new IntIterator() {
                private Node<K> nextNode = first, toRemove;
                private long expected = modifications;

                private void check() {
                    if (expected != modifications) throw new ConcurrentModificationException();
                }

                @Override
                public boolean hasNext() {
                    check();
                    return nextNode != null;
                }

                private Node<K> advance() {
                    if (!hasNext()) throw new NoSuchElementException();
                    final Node<K> node = nextNode;
                    nextNode = node.next;
                    toRemove = node;
                    return node;
                }

                @Override
                public int nextInt() {
                    return requiredValue(advance());
                }

                @Override
                @SuppressWarnings("deprecation")
                public Integer next() {
                    return boxedValue(advance());
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
            for (Node<K> node = first; node != null; node = node.next)
                if (!node.nullValue) hash += node.value;
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

    private final class ForwardEntry implements Reference2IntMap.Entry<K> {
        private final Cursor<?> cursor;
        private Node<K> node;

        ForwardEntry(final Node<K> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        public K getKey() {
            return boxedKey(node);
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getValue() {
            return boxedValue(node);
        }

        @Override
        public int getIntValue() {
            return requiredValue(node);
        }

        private Node<K> replace(final int replacement, final boolean isNull) {
            checkMutable();
            final int hash = smear(isNull ? 0 : replacement);
            if (node.valueHash == hash
                && (node.nullValue == isNull && (isNull || node.value == replacement)))
                return node;
            if (seekValue(replacement, isNull, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<K> previous = node;
            final Node<K> replacementNode =
                new Node<>(node.key, node.keyHash, replacement, isNull, hash);
            replaceNodeForKey(previous, replacementNode);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacementNode;
            node = replacementNode;
            return previous;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer setValue(final Integer value) {
            final Node<K> previous = replace(value == null ? 0 : value, value == null);
            if (previous == node) return value;
            return boxedValue(previous);
        }

        @Override
        public int setValue(final int value) {
            final Node<K> previous = replace(value, false);
            return previous.nullValue ? 0 : previous.value;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(node.key) ^ (node.nullValue ? 0 : node.value);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                && getKey() == entry.getKey()
                && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private final class InverseEntry implements Int2ReferenceMap.Entry<K> {
        private final Cursor<?> cursor;
        private Node<K> node;

        InverseEntry(final Node<K> node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getKey() {
            return boxedValue(node);
        }

        @Override
        public K getValue() {
            return boxedKey(node);
        }

        @Override
        public int getIntKey() {
            return requiredValue(node);
        }

        @Override
        public K setValue(final K value) {
            checkMutable();
            final K previousValue = node.key;
            final int hash = smear(System.identityHashCode(value));
            if (node.keyHash == hash && value == previousValue) return value;
            if (enumType != null && !enumType.isInstance(Objects.requireNonNull(value)))
                throw new ClassCastException("Expected " + enumType.getName());
            if (seekKey(value, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node<K> previous = node;
            final Node<K> replacement =
                new Node<>(value, hash, node.value, node.nullValue, node.valueHash);
            delete(previous);
            append(replacement);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacement;
            node = replacement;
            return previousValue;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(node.key) ^ (node.nullValue ? 0 : node.value);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                && Objects.equals(getKey(), entry.getKey())
                && getValue() == entry.getValue();
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
