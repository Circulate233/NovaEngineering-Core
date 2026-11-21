package github.kasuminova.novaeng.client.model;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Matrix4f;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Only factory-local matrices enter this pool. Caller-owned TRSR inputs are never replaced. */
public final class OwnedModelTransforms {
    private static final TransformType[] TYPES = TransformType.values();
    private static final int LIMIT = 16384;
    private final ConcurrentHashMap<MatrixKey, TRSRTransformation> matrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MapKey, ImmutableMap<TransformType, TRSRTransformation>> maps = new ConcurrentHashMap<>();
    private final Set<TRSRTransformation> owned = new ReferenceOpenHashSet<>();

    private OwnedModelTransforms() { }

    private static OwnedModelTransforms active() {
        return GeometryGeneration.attachment(OwnedModelTransforms.class, OwnedModelTransforms::new, OwnedModelTransforms::clear);
    }

    /** The argument must be newly computed private factory storage, with no surviving external alias. */
    public static TRSRTransformation create(final Matrix4f privateMatrix) {
        final OwnedModelTransforms pool = active();
        if (pool == null) return new TRSRTransformation(privateMatrix);
        final MatrixKey key = new MatrixKey(privateMatrix);
        final TRSRTransformation cached = pool.matrices.get(key);
        if (cached != null) return cached;
        if (pool.matrices.size() >= LIMIT) return new TRSRTransformation(privateMatrix);
        return pool.matrices.computeIfAbsent(key, ignored -> {
            final TRSRTransformation result = new TRSRTransformation(privateMatrix);
            synchronized (pool.owned) { pool.owned.add(result); }
            return result;
        });
    }

    public static ImmutableMap<TransformType, TRSRTransformation> share(final ImmutableMap<TransformType, TRSRTransformation> map) {
        final OwnedModelTransforms pool = active();
        if (pool == null) return map;
        synchronized (pool.owned) {
            for (final TransformType type : TYPES) {
                final TRSRTransformation transform = map.get(type);
                if (transform == null) continue;
                if (transform != TRSRTransformation.identity() && !pool.owned.contains(transform)) return map;
            }
        }
        final MapKey key = new MapKey(map);
        final ImmutableMap<TransformType, TRSRTransformation> cached = pool.maps.get(key);
        if (cached != null) return cached;
        if (pool.maps.size() >= LIMIT) return map;
        final ImmutableMap<TransformType, TRSRTransformation> previous = pool.maps.putIfAbsent(key, map);
        return previous == null ? map : previous;
    }

    private void clear() {
        matrices.clear();
        maps.clear();
        synchronized (owned) { owned.clear(); }
    }

    private static int bits(final Matrix4f m, final int i) {
        return Float.floatToRawIntBits(switch (i) {
            case 0 -> m.m00; case 1 -> m.m01; case 2 -> m.m02; case 3 -> m.m03;
            case 4 -> m.m10; case 5 -> m.m11; case 6 -> m.m12; case 7 -> m.m13;
            case 8 -> m.m20; case 9 -> m.m21; case 10 -> m.m22; case 11 -> m.m23;
            case 12 -> m.m30; case 13 -> m.m31; case 14 -> m.m32; case 15 -> m.m33;
            default -> throw new IndexOutOfBoundsException(i);
        });
    }

    private static final class MatrixKey {
        private final Matrix4f value;
        private final int hash;
        private MatrixKey(final Matrix4f value) {
            this.value = value;
            int result = 1;
            for (int i = 0; i < 16; i++) result = 31 * result + bits(value, i);
            hash = result;
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(final Object other) {
            if (!(other instanceof MatrixKey key) || hash != key.hash) return false;
            for (int i = 0; i < 16; i++) if (bits(value, i) != bits(key.value, i)) return false;
            return true;
        }
    }

    private static final class MapKey {
        private final ImmutableMap<TransformType, TRSRTransformation> value;
        private final int hash;
        private MapKey(final ImmutableMap<TransformType, TRSRTransformation> value) {
            this.value = value;
            int result = 1;
            for (final TransformType type : TYPES) result = 31 * result + System.identityHashCode(value.get(type));
            hash = result;
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(final Object other) {
            if (!(other instanceof MapKey key) || value.size() != key.value.size()) return false;
            for (final TransformType type : TYPES) if (value.get(type) != key.value.get(type)) return false;
            return true;
        }
    }
}
