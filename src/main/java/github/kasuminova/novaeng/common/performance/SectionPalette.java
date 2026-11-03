package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.Arrays;

/**
 * Reusable identity palette. Only snapshots handed to NBT allocate an owning result array.
 */
public final class SectionPalette<S> {
    private static final int HOT_CACHE_SIZE = 256;
    private static final int HOT_CACHE_MASK = HOT_CACHE_SIZE - 1;
    private final Reference2IntOpenHashMap<S> indices = new Reference2IntOpenHashMap<>(256);
    private final int[] globalIds = new int[SectionIdCodec.SIZE + 1];
    private final Object[] cachedStates = new Object[HOT_CACHE_SIZE];
    private final int[] cachedIndices = new int[HOT_CACHE_SIZE];
    private final int[] cachedEpochs = new int[HOT_CACHE_SIZE];
    private int cacheEpoch = 1;
    private S lastState;
    private int lastIndex;
    private boolean hasLast;

    public SectionPalette() {
        indices.defaultReturnValue(-1);
    }

    public int indexFor(final S state, final int globalId) {
        if (this.hasLast && state == this.lastState) return this.lastIndex;
        final int cacheSlot = globalId < 0 ? -1 : globalId * 0x9E3779B9 & HOT_CACHE_MASK;
        if (cacheSlot >= 0 && this.cachedEpochs[cacheSlot] == this.cacheEpoch
            && this.cachedStates[cacheSlot] == state) {
            this.lastState = state;
            this.lastIndex = this.cachedIndices[cacheSlot];
            this.hasLast = true;
            return this.lastIndex;
        }
        int index = indices.getInt(state);
        if (index < 0) {
            return this.addNew(state, globalId);
        }
        if (cacheSlot >= 0) {
            this.cachedStates[cacheSlot] = state;
            this.cachedIndices[cacheSlot] = index;
            this.cachedEpochs[cacheSlot] = this.cacheEpoch;
        }
        this.lastState = state;
        this.lastIndex = index;
        this.hasLast = true;
        return index;
    }

    int addNew(final S state, final int globalId) {
        final int index = indices.size();
        indices.put(state, index);
        globalIds[index] = globalId;
        final int cacheSlot = globalId < 0 ? -1 : globalId * 0x9E3779B9 & HOT_CACHE_MASK;
        if (cacheSlot >= 0) {
            this.cachedStates[cacheSlot] = state;
            this.cachedIndices[cacheSlot] = index;
            this.cachedEpochs[cacheSlot] = this.cacheEpoch;
        }
        this.lastState = state;
        this.lastIndex = index;
        this.hasLast = true;
        return index;
    }

    public int size() {
        return indices.size();
    }

    public int indexOf(final S state) {
        if (this.hasLast && state == this.lastState) return this.lastIndex;
        final int index = indices.getInt(state);
        if (index >= 0) {
            this.lastState = state;
            this.lastIndex = index;
            this.hasLast = true;
        }
        return index;
    }

    public int[] snapshot() {
        return Arrays.copyOf(globalIds, indices.size());
    }

    public void clear() {
        indices.clear();
        if (++this.cacheEpoch == 0) {
            Arrays.fill(this.cachedEpochs, 0);
            this.cacheEpoch = 1;
        }
        lastState = null;
        lastIndex = -1;
        hasLast = false;
    }
}
