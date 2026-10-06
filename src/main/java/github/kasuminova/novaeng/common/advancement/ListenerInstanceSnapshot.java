package github.kasuminova.novaeng.common.advancement;

import java.util.Collection;
import java.util.function.Function;

/**
 * Holds a listener collection and the per-listener value derived from it as two parallel arrays.
 *
 * <p>A walk that reads a derived value out of every element of a collection pays for that read once per walk, on top
 * of the iteration itself. When the collection only changes during registration and the derived value is fixed for a
 * given element, both costs can be paid once: the elements and the values read out of them are kept side by side, and
 * a walk becomes an indexed loop over the values.</p>
 *
 * <p>The snapshot is built from the collection on demand and dropped whenever the collection changes, so it is only
 * ever built from the collection as it currently stands. Nothing here observes the collection between builds - the
 * caller is the one that knows when it changed, and calls {@link #invalidate()} at that point. A build made from a
 * collection that is later changed without an invalidate would keep answering with the stale elements, which is the
 * one thing this class cannot detect on its own.</p>
 *
 * <p>Arrays rather than lists because the point is to remove allocation from the walk: {@link #size()} and the two
 * indexed readers allocate nothing, so a walk over a built snapshot does not allocate at all.</p>
 *
 * @param <L> the element type
 * @param <I> the value derived from an element
 */
public final class ListenerInstanceSnapshot<L, I> {

    /** Elements of the current snapshot, parallel to {@link #instances}; null when no snapshot is held. */
    private Object[] listeners;

    /** Values derived from {@link #listeners}; null when no snapshot is held. */
    private Object[] instances;

    /**
     * Whether a snapshot is currently held.
     *
     * @return true when {@link #size()} and the readers can be used
     */
    public boolean isHeld() {
        return this.listeners != null;
    }

    /**
     * Drops the snapshot, so the next {@link #rebuild} reads the collection again.
     *
     * <p>Called by whoever changes the collection. Dropping rather than rebuilding keeps a change that arrives during
     * a walk from doing work the walk will not use - the next walk rebuilds.</p>
     */
    public void invalidate() {
        this.listeners = null;
        this.instances = null;
    }

    /**
     * Reads the collection into both arrays, deriving the value of each element as it goes.
     *
     * <p>The collection is iterated exactly once here, so the arrays come out in the order the collection hands its
     * elements out, and each element's value is read exactly once - which is the read a walk would otherwise repeat.
     * An empty collection still builds, so a walk does not rebuild on every call while nothing is registered.</p>
     *
     * @param source the collection to snapshot
     * @param instanceOf reads the value of one element
     */
    public void rebuild(final Collection<L> source, final Function<L, I> instanceOf) {
        final int size = source.size();
        final Object[] listeners = new Object[size];
        final Object[] instances = new Object[size];

        int index = 0;
        for (final L listener : source) {
            listeners[index] = listener;
            instances[index] = instanceOf.apply(listener);
            index++;
        }

        this.instances = instances;
        this.listeners = listeners;
    }

    /**
     * The number of elements in the held snapshot.
     *
     * @return the snapshot size
     * @throws IllegalStateException when no snapshot is held
     */
    public int size() {
        return this.listeners.length;
    }

    /**
     * The element at an index of the held snapshot.
     *
     * @param index the index, in the order the collection handed the element out
     * @return the element
     * @throws IllegalStateException when no snapshot is held
     */
    @SuppressWarnings("unchecked")
    public L listener(final int index) {
        return (L) this.listeners[index];
    }

    /**
     * The value derived from the element at an index of the held snapshot.
     *
     * @param index the index the element sits at
     * @return the value that was read from the element when the snapshot was built
     * @throws IllegalStateException when no snapshot is held
     */
    @SuppressWarnings("unchecked")
    public I instance(final int index) {
        return (I) this.instances[index];
    }
}
