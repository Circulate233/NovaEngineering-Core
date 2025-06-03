package github.kasuminova.novaeng.common.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class FixedSizeDeque<E> {
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
        return new ArrayList<>(deque);
    }

    public E get(int index) {
        if (index < 0 || index >= deque.size()) {
            throw new IndexOutOfBoundsException();
        }
        return deque.stream().skip(index).findFirst().orElse(null);
    }

    public E getFirst() {
        if (deque.isEmpty()){
            return null;
        }
        return deque.getFirst();
    }

    public E getLast(){
        if (deque.isEmpty()){
            return null;
        }
        return deque.getLast();
    }

    public int size() {
        return deque.size();
    }
}