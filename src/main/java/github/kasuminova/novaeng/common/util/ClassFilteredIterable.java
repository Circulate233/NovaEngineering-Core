package github.kasuminova.novaeng.common.util;

import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
    public @NonNull Iterator<S> iterator() {
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
