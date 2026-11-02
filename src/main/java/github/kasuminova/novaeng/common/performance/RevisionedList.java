package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.AbstractList;
import java.util.Collection;
import java.util.RandomAccess;

/**
 * A List-compatible registry whose revisions include replacements and mutations through iterators/subLists.
 * JDK AbstractList supplies fail-fast nested views; storage itself uses fastutil.
 */
public final class RevisionedList<E> extends AbstractList<E> implements RandomAccess {
    private final ObjectArrayList<E> values;
    private volatile long revision;

    public RevisionedList(final Collection<? extends E> initial) {
        values = new ObjectArrayList<>(initial);
    }

    public long revision() {
        return revision;
    }

    @Override
    public E get(final int index) {
        return values.get(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public synchronized E set(final int index, final E value) {
        final E old = values.set(index, value);
        revision++;
        return old;
    }

    @Override
    public synchronized void add(final int index, final E value) {
        values.add(index, value);
        modCount++;
        revision++;
    }

    @Override
    public synchronized boolean add(final E value) {
        values.add(value);
        modCount++;
        revision++;
        return true;
    }

    @Override
    public synchronized boolean addAll(final int index, final Collection<? extends E> additions) {
        if (!values.addAll(index, additions)) {
            return false;
        }
        modCount++;
        revision++;
        return true;
    }

    @Override
    public synchronized boolean addAll(final @NonNull Collection<? extends E> additions) {
        return addAll(values.size(), additions);
    }

    @Override
    public synchronized E remove(final int index) {
        final E old = values.remove(index);
        modCount++;
        revision++;
        return old;
    }

    @Override
    protected synchronized void removeRange(final int from, final int to) {
        values.subList(from, to).clear();
        if (from != to) {
            modCount++;
            revision++;
        }
    }

    @Override
    public synchronized Object @NonNull [] toArray() {
        return values.toArray();
    }

    @Override
    public synchronized <T> T @NonNull [] toArray(final T @NonNull [] array) {
        return values.toArray(array);
    }
}
