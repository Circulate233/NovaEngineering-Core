package github.kasuminova.novaeng.client.model;

import it.unimi.dsi.fastutil.HashCommon;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

/** Read-mostly cache with release-published slots and striped write locks. */
final class ReadMostlyObjectCache<K, V> {
    private static final int STRIPE_COUNT = 256;
    private static final int MIN_STRIPE_CAPACITY = 32;
    private static final VarHandle SLOT = MethodHandles.arrayElementVarHandle(Object[].class);
    private final Stripe<K, V>[] stripes;
    private final int stripeMask;
    private final String name;
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicInteger peak = new AtomicInteger();
    private final AtomicInteger expansions = new AtomicInteger();

    @SuppressWarnings("unchecked")
    ReadMostlyObjectCache(final String name, final int expectedEntries) {
        this.name = name;
        final int stripeCount = stripeCount(expectedEntries);
        stripeMask = stripeCount - 1;
        stripes = (Stripe<K, V>[]) new Stripe<?, ?>[stripeCount];
        final int initialStripeCapacity = initialStripeCapacity(expectedEntries);
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new Stripe<>(this, initialStripeCapacity);
        }
    }

    V get(final K key) {
        return stripe(key).get(key);
    }

    Object lock(final K key) {
        return stripe(key);
    }

    void put(final K key, final V value) {
        final Stripe<K, V> stripe = stripe(key);
        if (stripe.put(key, value)) {
            final int currentSize = size.incrementAndGet();
            peak.accumulateAndGet(currentSize, Math::max);
        }
    }

    void clear() {
        for (Stripe<K, V> stripe : stripes) {
            synchronized (stripe) {
                stripe.clear();
            }
        }
        size.set(0);
        peak.set(0);
        expansions.set(0);
    }

    String report() {
        int capacity = 0;
        for (Stripe<K, V> stripe : stripes) {
            capacity += stripe.capacity();
        }
        return name + " size=" + size.get() + " peak=" + peak.get()
            + " capacity=" + capacity + " stripes=" + stripes.length
            + " expansions=" + expansions.get();
    }

    private int initialStripeCapacity(final int expectedEntries) {
        final int perStripe = Math.max(1, (expectedEntries + stripes.length - 1) / stripes.length);
        int capacity = MIN_STRIPE_CAPACITY;
        while (capacity - (capacity >> 2) < perStripe && capacity < (1 << 30)) {
            capacity <<= 1;
        }
        return capacity;
    }

    private static int stripeCount(final int expectedEntries) {
        final int target = Math.clamp(expectedEntries / 4, 1, STRIPE_COUNT);
        int count = 1;
        while ((count << 1) <= target) {
            count <<= 1;
        }
        return count;
    }

    private Stripe<K, V> stripe(final K key) {
        return stripes[HashCommon.mix(key.hashCode()) & stripeMask];
    }

    private static final class Stripe<K, V> {
        private final ReadMostlyObjectCache<K, V> owner;
        private final int initialCapacity;
        private volatile Table<K, V> table;

        private Stripe(final ReadMostlyObjectCache<K, V> owner, final int initialCapacity) {
            this.owner = owner;
            this.initialCapacity = initialCapacity;
            table = new Table<>(initialCapacity);
        }

        V get(final K key) {
            final Table<K, V> current = table;
            int index = HashCommon.mix(key.hashCode()) & current.mask;
            while (true) {
                final Object value = SLOT.getAcquire(current.values, index);
                if (value == null) {
                    return null;
                }
                if (key.equals(current.keys[index])) {
                    @SuppressWarnings("unchecked") final V result = (V) value;
                    return result;
                }
                index = (index + 1) & current.mask;
            }
        }

        boolean put(final K key, final V value) {
            Table<K, V> current = table;
            if (current.size + 1 > current.threshold) {
                final Table<K, V> previous = current;
                current = new Table<>(current.keys.length << 1);
                for (int i = 0; i < previous.keys.length; i++) {
                    final Object existing = previous.keys[i];
                    if (existing != null) {
                        current.insert(existing, previous.values[i]);
                    }
                }
                table = current;
                owner.expansions.incrementAndGet();
            }
            return current.insert(key, value);
        }

        void clear() {
            table = new Table<>(initialCapacity);
        }

        int capacity() {
            return table.keys.length;
        }
    }

    private static final class Table<K, V> {
        private final Object[] keys;
        private final Object[] values;
        private final int mask;
        private final int threshold;
        private volatile int size;

        private Table(final int capacity) {
            keys = new Object[capacity];
            values = new Object[capacity];
            mask = capacity - 1;
            threshold = capacity - (capacity >> 2);
        }

        private boolean insert(final Object key, final Object value) {
            int index = HashCommon.mix(key.hashCode()) & mask;
            while (SLOT.getAcquire(values, index) != null) {
                if (keys[index].equals(key)) {
                    SLOT.setRelease(values, index, value);
                    return false;
                }
                index = (index + 1) & mask;
            }
            keys[index] = key;
            SLOT.setRelease(values, index, value);
            size++;
            return true;
        }
    }
}
