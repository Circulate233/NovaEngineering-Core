package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.ObjectIterators;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;

public final class EmptyQueue<E> extends AbstractQueue<E> {

    private static final EmptyQueue<?> EMPTY = new EmptyQueue<>();

    private EmptyQueue() {
    }

    @SuppressWarnings("unchecked")
    public static <T> EmptyQueue<T> empty() {
        return (EmptyQueue<T>) EMPTY;
    }

    @Override
    public boolean add(final E value) {
        return false;
    }

    @Override
    public boolean offer(final E value) {
        return false;
    }

    @Override
    public E remove() {
        return null;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E element() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return ObjectIterators.emptyIterator();
    }

    @Override
    public boolean remove(final Object value) {
        return false;
    }

    @Override
    public boolean addAll(final Collection<? extends E> values) {
        return false;
    }

    @Override
    public boolean removeAll(final Collection<?> values) {
        return false;
    }

    @Override
    public boolean retainAll(final Collection<?> values) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean contains(final Object value) {
        return false;
    }

    @Override
    public boolean containsAll(final Collection<?> values) {
        return false;
    }
}
