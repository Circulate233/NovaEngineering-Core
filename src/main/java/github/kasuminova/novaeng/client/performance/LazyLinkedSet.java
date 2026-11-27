package github.kasuminova.novaeng.client.performance;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;

/** Event-owned Set: no shared mutable empty state, and no backing map until the first add. */
public final class LazyLinkedSet<E> extends AbstractSet<E> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private ObjectLinkedOpenHashSet<E> values;
    private transient int version;

    public static <E> Set<E> create() {
        return new LazyLinkedSet<>();
    }

    @Override
    public int size() {
        return values == null ? 0 : values.size();
    }

    @Override
    public boolean contains(final Object value) {
        return values != null && values.contains(value);
    }

    @Override
    public boolean add(final E value) {
        if (values == null) values = new ObjectLinkedOpenHashSet<>();
        if (!values.add(value)) return false;
        version++;
        return true;
    }

    @Override
    public boolean remove(final Object value) {
        if (values == null || !values.remove(value)) return false;
        version++;
        return true;
    }

    @Override
    public void clear() {
        if (values != null) values.clear();
        version++;
    }

    @Override
    public Iterator<E> iterator() {
        if (values != null) {
            final Iterator<E> iterator = values.iterator();
            return new Iterator<>() {
                private int expectedVersion = version;

                @Override
                public boolean hasNext() {
                    checkForModification();
                    return iterator.hasNext();
                }

                @Override
                public E next() {
                    checkForModification();
                    return iterator.next();
                }

                @Override
                public void remove() {
                    checkForModification();
                    iterator.remove();
                    expectedVersion = ++version;
                }

                private void checkForModification() {
                    if (expectedVersion != version) throw new ConcurrentModificationException();
                }
            };
        }
        final int expectedVersion = version;
        return new Iterator<>() {
            @Override public boolean hasNext() { return false; }
            @Override public E next() {
                if (expectedVersion != version) throw new ConcurrentModificationException();
                throw new NoSuchElementException();
            }
            @Override public void remove() { throw new IllegalStateException(); }
        };
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
