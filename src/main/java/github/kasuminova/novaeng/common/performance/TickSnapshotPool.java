package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterators;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TickSnapshotPool<T> {

    private static final int MAX_IDLE_ARRAYS = 2;
    private static final int MAX_IDLE_CAPACITY = 16384;
    private final ObjectArrayList<Object[]> idle = new ObjectArrayList<>(MAX_IDLE_ARRAYS);

    public <E> Iterator<T> select(final List<E> entries, final Predicate<? super E> accepts,
                                  final Function<? super E, ? extends T> value) {
        Object[] array = this.borrow(entries.size());
        int size = 0;
        try {
            for (final E entry : entries) {
                if (accepts.test(entry)) {
                    if (size == array.length) {
                        array = Arrays.copyOf(array, Math.max(16, array.length * 2));
                    }
                    array[size++] = value.apply(entry);
                }
            }
        } catch (final RuntimeException | Error failure) {
            this.recycle(array, size);
            throw failure;
        }
        if (size == 0) {
            this.recycle(array, size);
            return ObjectIterators.emptyIterator();
        }
        return new SnapshotIterator<>(this, array, size);
    }

    private synchronized Object[] borrow(final int capacity) {
        final Object[] array = this.idle.isEmpty() ? null : this.idle.removeLast();
        return array == null || array.length < capacity ? new Object[Math.max(16, capacity)] : array;
    }

    private void recycle(final Object[] array, final int size) {
        Arrays.fill(array, 0, size, null);
        if (array.length <= MAX_IDLE_CAPACITY) {
            synchronized (this) {
                if (this.idle.size() < MAX_IDLE_ARRAYS) {
                    this.idle.add(array);
                }
            }
        }
    }

    private static final class SnapshotIterator<T> implements Iterator<T> {
        private final TickSnapshotPool<T> pool;
        private Object[] array;
        private int size;
        private int index;
        private boolean removable;

        private SnapshotIterator(final TickSnapshotPool<T> pool, final Object[] array, final int size) {
            this.pool = pool;
            this.array = array;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            final T value = (T) this.array[this.index++];
            this.removable = true;
            if (this.index == this.size) {
                // An outstanding iterator owns its array; abandoned snapshots are not held by the pool.
                final Object[] completed = this.array;
                this.array = null;
                this.pool.recycle(completed, this.size);
            }
            return value;
        }

        @Override
        public void remove() {
            if (!this.removable) {
                throw new IllegalStateException();
            }
            this.removable = false;
            if (this.array != null) {
                System.arraycopy(this.array, this.index, this.array, this.index - 1, this.size - this.index);
                this.array[--this.size] = null;
                this.index--;
            }
        }
    }
}
