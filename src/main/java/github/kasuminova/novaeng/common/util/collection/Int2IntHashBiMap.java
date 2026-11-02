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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
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

/**
 * Primitive specialization of Guava 33.6.0 HashBiMap. Bucket chains, cached hashes,
 * insertion order and forward/inverse replacement rules follow the original implementation.
 */
// Backed views intentionally retain their map; fastutil supplies the inherited Cloneable marker.
@SuppressWarnings({"ReturnOfInnerClass", "CloneableClassInSecureContext"})
public final class Int2IntHashBiMap extends AbstractMap<Integer, Integer>
    implements Int2IntBiMap, Serializable {
    private final Inverse inverse = new Inverse();
    private transient Node[] keyTable, valueTable;
    private transient Node first, last;
    private transient int size, mask;
    private transient long modifications;
    private boolean frozen;
    private transient IntSet keyView;
    private transient IntSet valueView;
    private transient ObjectSet<Int2IntMap.Entry> entries;

    @SuppressWarnings("unused")
    public Int2IntHashBiMap() {
        this(16);
    }

    @SuppressWarnings("unused")
    public Int2IntHashBiMap(final int expectedSize) {
        init(expectedSize);
    }

    private static int smear(final int hash) {
        return Integer.rotateLeft(hash * 0xcc9e2d51, 15) * 0x1b873593;
    }

    public static Int2IntHashBiMap copyOf(final Map<? extends Integer, ? extends Integer> source) {
        final Int2IntHashBiMap result = new Int2IntHashBiMap(source.size());
        if (source instanceof Int2IntHashBiMap nativeMap) {
            for (Node node = nativeMap.first; node != null; node = node.next)
                result.append(
                    new Node(
                        node.key,
                        node.nullKey,
                        node.keyHash,
                        node.value,
                        node.nullValue,
                        node.valueHash));
        } else result.putAll(source);
        return result;
    }

    @SuppressWarnings("unused")
    public static Int2IntHashBiMap immutableCopyOf(
        final Map<? extends Integer, ? extends Integer> source) {
        final Int2IntHashBiMap result = copyOf(source);
        for (Node node = result.first; node != null; node = node.next) {
            if (node.nullKey || node.nullValue) throw new NullPointerException("null mapping");
        }
        result.frozen = true;
        return result;
    }

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

    private Node findKey(final int number, final boolean isNull) {
        return seekKey(number, isNull, smear(isNull ? 0 : number));
    }

    private Node findKeyBoxed(final Object value) {
        return value == null
            ? findKey(0, true)
            : value instanceof Integer number ? findKey(number, false) : null;
    }

    private Node seekKey(final int number, final boolean isNull, final int hash) {
        for (Node node = keyTable[hash & mask]; node != null; node = node.nextKey)
            if (node.keyHash == hash && node.nullKey == isNull && (isNull || node.key == number))
                return node;
        return null;
    }

    private Integer boxedKey(final Node node) {
        return node.nullKey ? null : node.key;
    }

    private int requiredKey(final Node node) {
        if (node == null || node.nullKey) throw new NullPointerException("null integer mapping");
        return node.key;
    }

    private Node findValue(final int number, final boolean isNull) {
        return seekValue(number, isNull, smear(isNull ? 0 : number));
    }

    private Node findValueBoxed(final Object value) {
        return value == null
            ? findValue(0, true)
            : value instanceof Integer number ? findValue(number, false) : null;
    }

    private Node seekValue(final int number, final boolean isNull, final int hash) {
        for (Node node = valueTable[hash & mask]; node != null; node = node.nextValue)
            if (node.valueHash == hash
                && node.nullValue == isNull
                && (isNull || node.value == number)) return node;
        return null;
    }

    private Integer boxedValue(final Node node) {
        return node.nullValue ? null : node.value;
    }

    private int requiredValue(final Node node) {
        if (node == null || node.nullValue) throw new NullPointerException("null integer mapping");
        return node.value;
    }

    private void delete(final Node node) {
        int bucket = node.keyHash & mask;
        Node previous = null;
        for (Node current = keyTable[bucket]; ; current = current.nextKey) {
            if (current == node) {
                if (previous == null) keyTable[bucket] = node.nextKey;
                else previous.nextKey = node.nextKey;
                break;
            }
            previous = current;
        }
        bucket = node.valueHash & mask;
        previous = null;
        for (Node current = valueTable[bucket]; ; current = current.nextValue) {
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

    private void index(final Node node) {
        final int keyBucket = node.keyHash & mask, valueBucket = node.valueHash & mask;
        node.nextKey = keyTable[keyBucket];
        keyTable[keyBucket] = node;
        node.nextValue = valueTable[valueBucket];
        valueTable[valueBucket] = node;
        size++;
        modifications++;
    }

    private void splice(final Node node, final Node before, final Node after) {
        index(node);
        node.prev = before;
        node.next = after;
        if (before == null) first = node;
        else before.next = node;
        if (after == null) last = node;
        else after.prev = node;
    }

    private void append(final Node node) {
        splice(node, last, null);
    }

    private void replaceNodeForKey(final Node oldNode, final Node newNode) {
        final Node before = oldNode.prev, after = oldNode.next;
        delete(oldNode);
        splice(newNode, before, after);
    }

    private void rehashIfNecessary() {
        if (size <= keyTable.length || keyTable.length == 1 << 30) return;
        final int capacity = keyTable.length * 2;
        keyTable = new Node[capacity];
        valueTable = new Node[capacity];
        mask = capacity - 1;
        size = 0;
        for (Node node = first; node != null; node = node.next) index(node);
        modifications++;
    }

    private Node putForward(
        final int key,
        final boolean keyNull,
        final int value,
        final boolean valueNull,
        final boolean force) {
        checkMutable();
        final int keyHash = smear(keyNull ? 0 : key), valueHash = smear(valueNull ? 0 : value);
        final Node oldKey = seekKey(key, keyNull, keyHash);
        if (oldKey != null
            && oldKey.valueHash == valueHash
            && (oldKey.nullValue == valueNull && (valueNull || oldKey.value == value)))
            return oldKey;
        final Node oldValue = seekValue(value, valueNull, valueHash);
        if (oldValue != null) {
            if (!force) throw new IllegalArgumentException("value already present");
            delete(oldValue);
        }
        final Node node = new Node(key, keyNull, keyHash, value, valueNull, valueHash);
        if (oldKey != null) {
            replaceNodeForKey(oldKey, node);
            return oldKey;
        }
        append(node);
        rehashIfNecessary();
        return null;
    }

    private Node putInverse(
        final int key,
        final boolean keyNull,
        final int value,
        final boolean valueNull,
        final boolean force) {
        checkMutable();
        final int valueHash = smear(valueNull ? 0 : value), keyHash = smear(keyNull ? 0 : key);
        final Node oldValue = seekValue(value, valueNull, valueHash);
        final Node oldKey = seekKey(key, keyNull, keyHash);
        if (oldValue != null
            && oldValue.keyHash == keyHash
            && (oldValue.nullKey == keyNull && (keyNull || oldValue.key == key)))
            return oldValue;
        if (oldKey != null && !force) throw new IllegalArgumentException("key already present");
        if (oldValue != null) delete(oldValue);
        final Node node = new Node(key, keyNull, keyHash, value, valueNull, valueHash);
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
    public Int2IntBiMap inverse() {
        return inverse;
    }

    @Override
    public boolean containsKey(final Object key) {
        return findKeyBoxed(key) != null;
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
        final Node node = findKeyBoxed(key);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public Integer getOrDefault(final Object key, final Integer fallback) {
        final Node node = findKeyBoxed(key);
        return node == null ? fallback : boxedValue(node);
    }

    @Override
    public Integer put(final Integer key, final Integer value) {
        return putBoxed(key, value, false);
    }

    @Override
    public Integer forcePut(final Integer key, final Integer value) {
        return putBoxed(key, value, true);
    }

    private Integer putBoxed(final Integer key, final Integer value, final boolean force) {
        final Node previous =
            putForward(
                key == null ? 0 : key,
                key == null,
                value == null ? 0 : value,
                value == null,
                force);
        if (previous == null) return null;
        if (value == null
            ? previous.nullValue
            : !previous.nullValue && previous.value == value) return value;
        return boxedValue(previous);
    }

    @Override
    public Integer remove(final Object key) {
        checkMutable();
        final Node node = findKeyBoxed(key);
        if (node == null) return null;
        final Integer value = boxedValue(node);
        delete(node);
        return value;
    }

    @Override
    public boolean containsKey(final int key) {
        return findKey(key, false) != null;
    }

    @Override
    public boolean containsValue(final int value) {
        return findValue(value, false) != null;
    }

    @Override
    public int get(final int key) {
        return getOrDefault(key, 0);
    }

    @Override
    public int getOrDefault(final int key, final int fallback) {
        final Node node = findKey(key, false);
        return node == null ? fallback : requiredValue(node);
    }

    @Override
    public int getRequired(final int key) {
        return requiredValue(findKey(key, false));
    }

    @Override
    public int put(final int key, final int value) {
        return putPrimitive(key, value, false);
    }

    @Override
    public int forcePut(final int key, final int value) {
        return putPrimitive(key, value, true);
    }

    private int putPrimitive(final int key, final int value, final boolean force) {
        final Node previous = putForward(key, false, value, false, force);
        return previous == null || previous.nullValue ? 0 : previous.value;
    }

    @Override
    public void putDiscard(final int key, final int value, final boolean force) {
        putForward(key, false, value, false, force);
    }

    @Override
    public int removeRequired(final int key) {
        checkMutable();
        final Node node = findKey(key, false);
        final boolean missing = node == null || node.nullValue;
        final int value = node == null ? 0 : node.value;
        if (node != null) delete(node);
        if (missing) throw new NullPointerException("null integer mapping");
        return value;
    }

    @Override
    public Integer getBoxed(final int key) {
        final Node node = findKey(key, false);
        return node == null ? null : boxedValue(node);
    }

    @Override
    public int remove(final int key) {
        checkMutable();
        final Node node = findKey(key, false);
        final int result = (node == null || node.nullValue ? 0 : node.value);
        if (node != null) delete(node);
        return result;
    }

    @Override
    public void putAll(final @NotNull Map<? extends Integer, ? extends Integer> source) {
        checkMutable();
        if (source instanceof Int2IntHashBiMap nativeMap) {
            for (Node node = nativeMap.first; node != null; node = node.next)
                putForward(node.key, node.nullKey, node.value, node.nullValue, false);
            return;
        }

        for (final Map.Entry<? extends Integer, ? extends Integer> entry : source.entrySet())
            put(entry.getKey(), entry.getValue());
    }

    @Override
    public void forEach(final BiConsumer<? super Integer, ? super Integer> consumer) {
        Objects.requireNonNull(consumer);
        for (Node node = first; node != null; node = node.next)
            consumer.accept(boxedKey(node), boxedValue(node));
    }

    @Override
    public void replaceAll(
        final BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
        checkMutable();
        Objects.requireNonNull(function);
        final Node original = first;
        clear();
        for (Node node = original; node != null; node = node.next)
            put(boxedKey(node), function.apply(boxedKey(node), boxedValue(node)));
    }

    @Override
    public Integer putIfAbsent(final Integer key, final Integer value) {
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
    public boolean replace(final Integer key, final Integer oldValue, final Integer newValue) {
        checkMutable();
        if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
        put(key, newValue);
        return true;
    }

    @Override
    public Integer replace(final Integer key, final Integer value) {
        checkMutable();
        return containsKey(key) ? put(key, value) : null;
    }

    @Override
    public Integer computeIfAbsent(
        final Integer key, @NotNull Function<? super Integer, ? extends Integer> function) {
        checkMutable();
        Objects.requireNonNull(function);
        Integer value = get(key);
        if (value == null && (value = function.apply(key)) != null) put(key, value);
        return value;
    }

    @Override
    public Integer computeIfPresent(
        final Integer key,
        @NotNull BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
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
        final Integer key,
        @NotNull BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
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
        final Integer key,
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
    public void forEachIntInt(final Int2IntBiMap.IntIntConsumer consumer) {
        Objects.requireNonNull(consumer);
        for (Node node = first; node != null; node = node.next)
            consumer.accept(requiredKey(node), requiredValue(node));
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
    public ObjectSet<Map.Entry<Integer, Integer>> entrySet() {
        return (ObjectSet) int2IntEntrySet();
    }

    @Override
    public ObjectSet<Int2IntMap.Entry> int2IntEntrySet() {
        if (entries == null) {
            final ObjectSet<Int2IntMap.Entry> view =
                new AbstractObjectSet<>() {
                    @Override
                    public int size() {
                        return size;
                    }

                    @Override
                    public void clear() {
                        Int2IntHashBiMap.this.clear();
                    }

                    @Override
                    public boolean contains(final Object value) {
                        if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                        final Node node = findKeyBoxed(entry.getKey());
                        return node != null
                            && Objects.equals(boxedValue(node), entry.getValue());
                    }

                    @Override
                    public boolean remove(final Object value) {
                        checkMutable();
                        if (!contains(value)) return false;
                        Int2IntHashBiMap.this.remove(((Map.Entry<?, ?>) value).getKey());
                        return true;
                    }

                    @Override
                    @NotNull
                    public ObjectIterator<Int2IntMap.Entry> iterator() {
                        return new Cursor<>() {
                            @Override
                            Int2IntMap.Entry output(final Node node) {
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
        for (Node node = first; node != null; node = node.next) {
            stream.writeBoolean(node.nullKey);
            stream.writeInt(node.key);
            stream.writeBoolean(node.nullValue);
            stream.writeInt(node.value);
        }
    }

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
            final boolean valueNull = stream.readBoolean();
            final int value = stream.readInt();
            putForward(key, keyNull, value, valueNull, false);
        }
        frozen = wasFrozen;
    }

    private static final class Node {
        final int keyHash, valueHash;
        final int key;
        final int value;
        final boolean nullKey;
        final boolean nullValue;
        Node nextKey, nextValue, prev, next;

        Node(
            final int key,
            final boolean nullKey,
            final int keyHash,
            final int value,
            final boolean nullValue,
            final int valueHash) {
            this.key = key;
            this.value = value;
            this.keyHash = keyHash;
            this.valueHash = valueHash;
            this.nullKey = nullKey;
            this.nullValue = nullValue;
        }
    }

    private final class Inverse extends AbstractMap<Integer, Integer>
        implements Int2IntBiMap, Serializable {
        private transient ObjectSet<Int2IntMap.Entry> inverseEntries;

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
        public Int2IntBiMap inverse() {
            return Int2IntHashBiMap.this;
        }

        @Override
        public void clear() {
            Int2IntHashBiMap.this.clear();
        }

        @Override
        public boolean containsKey(final Object key) {
            return findValueBoxed(key) != null;
        }

        @Override
        public boolean containsValue(final Object value) {
            return findKeyBoxed(value) != null;
        }

        @Override
        public Integer get(final Object key) {
            final Node node = findValueBoxed(key);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public Integer getOrDefault(final Object key, final Integer fallback) {
            final Node node = findValueBoxed(key);
            return node == null ? fallback : boxedKey(node);
        }

        @Override
        public Integer put(final Integer key, final Integer value) {
            return putBoxed(key, value, false);
        }

        @Override
        public Integer forcePut(final Integer key, final Integer value) {
            return putBoxed(key, value, true);
        }

        private Integer putBoxed(final Integer key, final Integer value, final boolean force) {
            final Node previous =
                putInverse(
                    value == null ? 0 : value,
                    value == null,
                    key == null ? 0 : key,
                    key == null,
                    force);
            if (previous == null) return null;
            if (value == null
                ? previous.nullKey
                : !previous.nullKey && previous.key == value) return value;
            return boxedKey(previous);
        }

        @Override
        public Integer remove(final Object key) {
            checkMutable();
            final Node node = findValueBoxed(key);
            if (node == null) return null;
            final Integer value = boxedKey(node);
            delete(node);
            return value;
        }

        @Override
        public boolean containsKey(final int key) {
            return findValue(key, false) != null;
        }

        @Override
        public boolean containsValue(final int value) {
            return findKey(value, false) != null;
        }

        @Override
        public int get(final int key) {
            return getOrDefault(key, 0);
        }

        @Override
        public int getOrDefault(final int key, final int fallback) {
            final Node node = findValue(key, false);
            return node == null ? fallback : requiredKey(node);
        }

        @Override
        public int getRequired(final int key) {
            return requiredKey(findValue(key, false));
        }

        @Override
        public int put(final int key, final int value) {
            return putPrimitive(key, value, false);
        }

        @Override
        public int forcePut(final int key, final int value) {
            return putPrimitive(key, value, true);
        }

        private int putPrimitive(final int key, final int value, final boolean force) {
            final Node previous = putInverse(value, false, key, false, force);
            return previous == null || previous.nullKey ? 0 : previous.key;
        }

        @Override
        public void putDiscard(final int key, final int value, final boolean force) {
            putInverse(value, false, key, false, force);
        }

        @Override
        public int removeRequired(final int key) {
            checkMutable();
            final Node node = findValue(key, false);
            final boolean missing = node == null || node.nullKey;
            final int value = node == null ? 0 : node.key;
            if (node != null) delete(node);
            if (missing) throw new NullPointerException("null integer mapping");
            return value;
        }

        @Override
        public Integer getBoxed(final int key) {
            final Node node = findValue(key, false);
            return node == null ? null : boxedKey(node);
        }

        @Override
        public int remove(final int key) {
            checkMutable();
            final Node node = findValue(key, false);
            final int result = (node == null || node.nullKey ? 0 : node.key);
            if (node != null) delete(node);
            return result;
        }

        @Override
        public void putAll(final Map<? extends Integer, ? extends Integer> source) {
            checkMutable();
            for (final Map.Entry<? extends Integer, ? extends Integer> entry : source.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public void forEach(final BiConsumer<? super Integer, ? super Integer> consumer) {
            Objects.requireNonNull(consumer);
            for (Node node = first; node != null; node = node.next)
                consumer.accept(boxedValue(node), boxedKey(node));
        }

        @Override
        public void replaceAll(
            final BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            final Node original = first;
            clear();
            for (Node node = original; node != null; node = node.next)
                put(boxedValue(node), function.apply(boxedValue(node), boxedKey(node)));
        }

        @Override
        public Integer putIfAbsent(final Integer key, final Integer value) {
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
        public boolean replace(final Integer key, final Integer oldValue, final Integer newValue) {
            checkMutable();
            if (!containsKey(key) || !Objects.equals(get(key), oldValue)) return false;
            put(key, newValue);
            return true;
        }

        @Override
        public Integer replace(final Integer key, final Integer value) {
            checkMutable();
            return containsKey(key) ? put(key, value) : null;
        }

        @Override
        public Integer computeIfAbsent(
            final Integer key, @NotNull Function<? super Integer, ? extends Integer> function) {
            checkMutable();
            Objects.requireNonNull(function);
            Integer value = get(key);
            if (value == null && (value = function.apply(key)) != null) put(key, value);
            return value;
        }

        @Override
        public Integer computeIfPresent(
            final Integer key,
            @NotNull BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
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
            final Integer key,
            @NotNull BiFunction<? super Integer, ? super Integer, ? extends Integer> function) {
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
            final Integer key,
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
        public void forEachIntInt(final Int2IntBiMap.IntIntConsumer consumer) {
            Objects.requireNonNull(consumer);
            for (Node node = first; node != null; node = node.next)
                consumer.accept(requiredValue(node), requiredKey(node));
        }

        @Override
        @NotNull
        public IntSet keySet() {
            return Int2IntHashBiMap.this.values();
        }

        @Override
        @NotNull
        public IntSet values() {
            return Int2IntHashBiMap.this.keySet();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        @NotNull
        public ObjectSet<Map.Entry<Integer, Integer>> entrySet() {
            return (ObjectSet) int2IntEntrySet();
        }

        @Override
        public ObjectSet<Int2IntMap.Entry> int2IntEntrySet() {
            if (inverseEntries == null) {
                final ObjectSet<Int2IntMap.Entry> view =
                    new AbstractObjectSet<>() {
                        @Override
                        public int size() {
                            return size;
                        }

                        @Override
                        public void clear() {
                            Int2IntHashBiMap.this.clear();
                        }

                        @Override
                        public boolean contains(final Object value) {
                            if (!(value instanceof Map.Entry<?, ?> entry)) return false;
                            final Node node = findValueBoxed(entry.getKey());
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
                        public ObjectIterator<Int2IntMap.Entry> iterator() {
                            return new Cursor<>() {
                                @Override
                                Int2IntMap.Entry output(final Node node) {
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
        private Node nextNode = first, toRemove;
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
            final Node node = nextNode;
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

        abstract T output(Node node);
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
            Int2IntHashBiMap.this.clear();
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
            final Node node = findKey(value, false);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final Object value) {
            checkMutable();
            final Node node = findKeyBoxed(value);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @NotNull
        public IntIterator iterator() {
            return new IntIterator() {
                private Node nextNode = first, toRemove;
                private long expected = modifications;

                private void check() {
                    if (expected != modifications) throw new ConcurrentModificationException();
                }

                @Override
                public boolean hasNext() {
                    check();
                    return nextNode != null;
                }

                private Node advance() {
                    if (!hasNext()) throw new NoSuchElementException();
                    final Node node = nextNode;
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
            for (Node node = first; node != null; node = node.next)
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
            Int2IntHashBiMap.this.clear();
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
            final Node node = findValue(value, false);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final Object value) {
            checkMutable();
            final Node node = findValueBoxed(value);
            if (node == null) return false;
            delete(node);
            return true;
        }

        @Override
        @NotNull
        public IntIterator iterator() {
            return new IntIterator() {
                private Node nextNode = first, toRemove;
                private long expected = modifications;

                private void check() {
                    if (expected != modifications) throw new ConcurrentModificationException();
                }

                @Override
                public boolean hasNext() {
                    check();
                    return nextNode != null;
                }

                private Node advance() {
                    if (!hasNext()) throw new NoSuchElementException();
                    final Node node = nextNode;
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
            for (Node node = first; node != null; node = node.next)
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

    private final class ForwardEntry implements Int2IntMap.Entry {
        private final Cursor<?> cursor;
        private Node node;

        ForwardEntry(final Node node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getKey() {
            return boxedKey(node);
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getValue() {
            return boxedValue(node);
        }

        @Override
        public int getIntKey() {
            return requiredKey(node);
        }

        @Override
        public int getIntValue() {
            return requiredValue(node);
        }

        private Node replace(final int replacement, final boolean isNull) {
            checkMutable();
            final int hash = smear(isNull ? 0 : replacement);
            if (node.valueHash == hash
                && (node.nullValue == isNull && (isNull || node.value == replacement)))
                return node;
            if (seekValue(replacement, isNull, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node previous = node;
            final Node replacementNode =
                new Node(node.key, node.nullKey, node.keyHash, replacement, isNull, hash);
            replaceNodeForKey(previous, replacementNode);
            cursor.expected = modifications;
            if (cursor.toRemove == previous) cursor.toRemove = replacementNode;
            node = replacementNode;
            return previous;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer setValue(final Integer value) {
            final Node previous = replace(value == null ? 0 : value, value == null);
            if (previous == node) return value;
            return boxedValue(previous);
        }

        @Override
        public int setValue(final int value) {
            final Node previous = replace(value, false);
            return previous.nullValue ? 0 : previous.value;
        }

        @Override
        public int hashCode() {
            return (node.nullKey ? 0 : node.key) ^ (node.nullValue ? 0 : node.value);
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

    private final class InverseEntry implements Int2IntMap.Entry {
        private final Cursor<?> cursor;
        private Node node;

        InverseEntry(final Node node, final Cursor<?> cursor) {
            this.node = node;
            this.cursor = cursor;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getKey() {
            return boxedValue(node);
        }

        @Override
        @SuppressWarnings("deprecation")
        public Integer getValue() {
            return boxedKey(node);
        }

        @Override
        public int getIntKey() {
            return requiredValue(node);
        }

        @Override
        public int getIntValue() {
            return requiredKey(node);
        }

        private Node replace(final int replacement, final boolean isNull) {
            checkMutable();
            final int hash = smear(isNull ? 0 : replacement);
            if (node.keyHash == hash
                && (node.nullKey == isNull && (isNull || node.key == replacement))) return node;
            if (seekKey(replacement, isNull, hash) != null)
                throw new IllegalArgumentException("value already present");
            final Node previous = node;
            final Node replacementNode =
                new Node(replacement, isNull, hash, node.value, node.nullValue, node.valueHash);
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
            final Node previous = replace(value == null ? 0 : value, value == null);
            if (previous == node) return value;
            return boxedKey(previous);
        }

        @Override
        public int setValue(final int value) {
            final Node previous = replace(value, false);
            return previous.nullKey ? 0 : previous.key;
        }

        @Override
        public int hashCode() {
            return (node.nullKey ? 0 : node.key) ^ (node.nullValue ? 0 : node.value);
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
