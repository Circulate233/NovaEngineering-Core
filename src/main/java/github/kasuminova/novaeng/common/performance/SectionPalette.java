package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.Arrays;

/**
 * Reusable identity palette. Only snapshots handed to NBT allocate an owning result array.
 */
public final class SectionPalette<S> {
    private final Reference2IntOpenHashMap<S> indices = new Reference2IntOpenHashMap<>(256);
    private final int[] globalIds = new int[SectionIdCodec.SIZE + 1];

    public SectionPalette() {
        indices.defaultReturnValue(-1);
    }

    public int indexFor(final S state, final int globalId) {
        int index = indices.getInt(state);
        if (index < 0) {
            index = indices.size();
            indices.put(state, index);
            globalIds[index] = globalId;
        }
        return index;
    }

    public int size() {
        return indices.size();
    }

    public int indexOf(final S state) {
        return indices.getInt(state);
    }

    public int[] snapshot() {
        return Arrays.copyOf(globalIds, indices.size());
    }

    public void clear() {
        indices.clear();
    }
}
