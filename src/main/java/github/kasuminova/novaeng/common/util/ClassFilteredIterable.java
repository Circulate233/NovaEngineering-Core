package github.kasuminova.novaeng.common.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Walks the entities of one class in one chunk section by index, without building an iterator for the backing list.
 *
 * <p>This stands in for what {@code ClassInheritanceMultiMap.getByClass} used to return: an {@code Iterable} whose
 * {@code iterator()} asked the section's list for an iterator ({@code ArrayList$Itr}) and then wrapped it in
 * {@code Iterators.filter}. Three short-lived objects per query, on the path every
 * {@code World.getEntitiesWithinAABB} takes - which is a query the client and the server both run constantly, and the
 * single largest allocator on the server thread once the world is up. Walking the list by index costs none of them,
 * and the filter is applied by the same {@code isInstance} test {@code Iterators.filter} used, so a list that somehow
 * holds an element of another class is still skipped rather than returned.</p>
 *
 * <p>One instance serves as both the {@code Iterable} and its {@code Iterator}: every vanilla caller iterates the
 * result exactly once, and the first {@code iterator()} call hands out this instance. A second call would otherwise
 * find an exhausted iteration, so it gets a fresh walk of the same list instead - the class keeps working for a caller
 * that iterates twice, at the cost of the allocation only in that case.</p>
 */
public final class ClassFilteredIterable<T, S> implements Iterable<S>, Iterator<S> {

    private final List<T> list;
    private final Class<S> type;
    private int index;
    private boolean handedOut;
    private S next;
    private boolean hasNext;

    public ClassFilteredIterable(final List<T> list, final Class<S> type) {
        this.list = list;
        this.type = type;
    }

    @Override
    public Iterator<S> iterator() {
        if (this.handedOut) {
            return new ClassFilteredIterable<>(this.list, this.type);
        }
        this.handedOut = true;
        return this;
    }

    @Override
    public boolean hasNext() {
        advance();
        return this.hasNext;
    }

    @Override
    public S next() {
        advance();
        if (!this.hasNext) {
            throw new NoSuchElementException();
        }
        final S result = this.next;
        this.hasNext = false;
        return result;
    }

    /**
     * {@code Iterators.filter} answered with an {@code AbstractIterator}, which does not support removal either.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void advance() {
        if (this.hasNext) {
            return;
        }
        while (this.index < this.list.size()) {
            final T candidate = this.list.get(this.index++);
            if (this.type.isInstance(candidate)) {
                this.next = this.type.cast(candidate);
                this.hasNext = true;
                return;
            }
        }
    }
}
