package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

import java.util.NoSuchElementException;

/**
 * Keeps dequeue allocation-free and reclaims burst capacity at lighting pass boundaries.
 */
public final class RetainedLongFifoQueue extends LongArrayFIFOQueue {
    private static final long serialVersionUID = 1L;
    private transient int sparsePasses;

    public RetainedLongFifoQueue(final int capacity) {
        super(capacity);
    }

    @Override
    public long dequeueLong() {
        if (start == end) throw new NoSuchElementException();
        final long result = array[start];
        if (++start == length) start = 0;
        return result;
    }

    @Override
    public long dequeueLastLong() {
        if (start == end) throw new NoSuchElementException();
        if (end == 0) end = length;
        return array[--end];
    }

    public void nextPass() {
        final int size = size();
        final int required = Math.max(257, size + 1);
        if (length / 8 >= required) sparsePasses++;
        else sparsePasses = 0;
        if (sparsePasses < 8 && (length <= 65536 || length / 2 < required)) return;
        final long[] resized = new long[required];
        final int first = Math.min(size, length - start);
        System.arraycopy(array, start, resized, 0, first);
        System.arraycopy(array, 0, resized, first, size - first);
        array = resized;
        length = required;
        start = 0;
        end = size;
        sparsePasses = 0;
    }
}
