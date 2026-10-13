package github.kasuminova.novaeng.client.model;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;

import java.util.Arrays;
import java.util.Map;

/** Ordered identity key; selector order affects 1.12.2 random seeds and first-model metadata. */
final class MultipartModelKey {
    private static final ThreadLocal<MultipartModelKey> LOOKUP = ThreadLocal.withInitial(MultipartModelKey::new);
    private Object[] predicates;
    private Object[] models;
    private int size;
    private long generation;
    private int hash;

    private MultipartModelKey() {
        predicates = new Object[4];
        models = new Object[4];
    }

    private MultipartModelKey(final Object[] predicates, final Object[] models, final int size,
                              final long generation, final int hash) {
        this.predicates = predicates;
        this.models = models;
        this.size = size;
        this.generation = generation;
        this.hash = hash;
    }

    static MultipartModelKey from(final Map<Predicate<IBlockState>, IBakedModel> selectors, final long generation) {
        final MultipartModelKey result = LOOKUP.get();
        result.size = selectors.size();
        if (result.size > result.predicates.length) {
            final int capacity = Integer.highestOneBit(result.size - 1) << 1;
            result.predicates = Arrays.copyOf(result.predicates, capacity);
            result.models = Arrays.copyOf(result.models, capacity);
        }
        int index = 0;
        for (Map.Entry<Predicate<IBlockState>, IBakedModel> entry : selectors.entrySet()) {
            result.predicates[index] = entry.getKey();
            result.models[index++] = entry.getValue();
        }
        result.generation = generation;
        int keyHash = 1;
        for (int i = 0; i < result.size; i++) {
            keyHash = 31 * keyHash + System.identityHashCode(result.predicates[i]);
            keyHash = 31 * keyHash + System.identityHashCode(result.models[i]);
        }
        result.hash = 31 * keyHash + Long.hashCode(generation);
        return result;
    }

    MultipartModelKey snapshot() {
        return new MultipartModelKey(Arrays.copyOf(predicates, size), Arrays.copyOf(models, size), size,
            generation, hash);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MultipartModelKey other) || generation != other.generation || size != other.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (predicates[i] != other.predicates[i] || models[i] != other.models[i]) {
                return false;
            }
        }
        return true;
    }
}
