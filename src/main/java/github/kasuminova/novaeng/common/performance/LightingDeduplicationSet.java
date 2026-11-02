package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class LightingDeduplicationSet extends LongOpenHashSet {

    private static final long serialVersionUID = 1L;
    private static final int MAX_RETAINED_TABLE_SIZE = 1 << 16;
    private static final int MIN_RETAINED_TABLE_SIZE = 256;
    private static final int SPARSE_PASSES_BEFORE_SHRINK = 8;
    private transient int sparsePasses;

    public LightingDeduplicationSet(final int expected) {
        super(expected);
    }

    @Override
    public void clear() {
        clearForExpected(0);
    }

    private void clearForExpected(final int expected) {
        final int required = Math.max(MIN_RETAINED_TABLE_SIZE,
            HashCommon.arraySize(Math.max(size, expected), f));
        if ((n >>> 3) >= required) {
            sparsePasses++;
        } else {
            sparsePasses = 0;
        }
        super.clear();
        if (sparsePasses >= SPARSE_PASSES_BEFORE_SHRINK) {
            // Resize only at an empty pass boundary, never in the enqueue hot path.
            super.rehash(required);
            sparsePasses = 0;
        }
    }

    public LightingDeduplicationSet nextPass(final int expected) {
        if (this.n > MAX_RETAINED_TABLE_SIZE && this.n > HashCommon.arraySize(expected, this.f)) {
            return new LightingDeduplicationSet(expected);
        }
        this.clearForExpected(expected);
        this.ensureCapacity(expected);
        return this;
    }
}
