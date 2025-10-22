package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class FixedSizeDeque<E> implements Iterable<E> {
    private final Deque<E> deque;
    private final int maxSize;

    public FixedSizeDeque(int maxSize) {
        this.maxSize = maxSize;
        this.deque = new ArrayDeque<>(maxSize);
    }

    public void addFirst(E element) {
        deque.addFirst(element);
        if (deque.size() > maxSize) {
            deque.removeLast();
        }
    }

    public List<E> getList() {
        return new ObjectArrayList<>(deque);
    }

    public E get(int index) {
        if (index < 0 || index >= deque.size()) {
            throw new IndexOutOfBoundsException();
        }
        return deque.stream().skip(index).findFirst().orElse(null);
    }

    public E getFirst() {
        if (deque.isEmpty()) {
            return null;
        }
        return deque.getFirst();
    }

    public E getLast() {
        if (deque.isEmpty()) {
            return null;
        }
        return deque.getLast();
    }

    public int size() {
        return deque.size();
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return deque.iterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        deque.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return deque.spliterator();
    }
}