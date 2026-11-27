package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Fastutil heap queue with a fail-fast iterator over the same heap order exposed by JDK PriorityQueue.
 */
public final class ObjectHeapPriorityQueueWithIterator<K> extends ObjectHeapPriorityQueue<K> implements Iterable<K> {
    private int modifications;

    public ObjectHeapPriorityQueueWithIterator() {
    }

    public ObjectHeapPriorityQueueWithIterator(final Comparator<? super K> comparator) {
        super(comparator);
    }

    @Override
    public void enqueue(final K element) {
        super.enqueue(element);
        modifications++;
    }

    @Override
    public K dequeue() {
        final K result = super.dequeue();
        modifications++;
        return result;
    }

    @Override
    public void changed() {
        super.changed();
        modifications++;
    }

    @Override
    public void clear() {
        super.clear();
        modifications++;
    }

    @Override
    public Iterator<K> iterator() {
        final int expectedModifications = modifications;
        final int expectedSize = size;
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                checkForModification();
                return index < expectedSize;
            }

            @Override
            public K next() {
                checkForModification();
                if (index >= expectedSize) throw new NoSuchElementException();
                return heap[index++];
            }

            private void checkForModification() {
                if (modifications != expectedModifications) throw new ConcurrentModificationException();
            }
        };
    }
}
