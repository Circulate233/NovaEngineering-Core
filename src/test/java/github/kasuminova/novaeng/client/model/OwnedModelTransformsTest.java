package github.kasuminova.novaeng.client.model;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.Test;

import javax.vecmath.Matrix4f;

import static org.junit.jupiter.api.Assertions.*;

class OwnedModelTransformsTest {
    private static Matrix4f matrix() {
        final Matrix4f result = new Matrix4f();
        result.setIdentity();
        result.m03 = .25F;
        return result;
    }

    @Test
    void onlyOwnedMatricesAndMapsShareWithinTheirBakeGeneration() {
        final TRSRTransformation first;
        try (GeometryGeneration ignored = GeometryGeneration.begin()) {
            first = OwnedModelTransforms.create(matrix());
            assertSame(first, OwnedModelTransforms.create(matrix()));
            final Matrix4f returned = first.getMatrix();
            returned.m03 = 97;
            assertEquals(.25F, first.getMatrix().m03);
            final var map = ImmutableMap.of(TransformType.GUI, first);
            assertSame(map, OwnedModelTransforms.share(map));
            assertSame(map, OwnedModelTransforms.share(ImmutableMap.of(TransformType.GUI, first)));
            final Matrix4f callerMatrix = matrix();
            final var external = ImmutableMap.of(TransformType.GUI, new TRSRTransformation(callerMatrix));
            assertSame(external, OwnedModelTransforms.share(external));
            callerMatrix.m03 = 87;
            assertEquals(87, external.get(TransformType.GUI).getMatrix().m03);
        }
        try (GeometryGeneration ignored = GeometryGeneration.begin()) {
            assertNotSame(first, OwnedModelTransforms.create(matrix()));
        }
    }

    @Test
    void exactFloatBitsAreNeverFoldedOrRounded() {
        try (GeometryGeneration ignored = GeometryGeneration.begin()) {
            final Matrix4f negativeZero = matrix();
            negativeZero.m10 = -0F;
            assertNotSame(OwnedModelTransforms.create(matrix()), OwnedModelTransforms.create(negativeZero));
            final Matrix4f a = matrix();
            a.m11 = Float.intBitsToFloat(0x7fc00001);
            final Matrix4f b = matrix();
            b.m11 = Float.intBitsToFloat(0x7fc00002);
            assertNotSame(OwnedModelTransforms.create(a), OwnedModelTransforms.create(b));
            assertEquals(0x7fc00001, Float.floatToRawIntBits(OwnedModelTransforms.create(a).getMatrix().m11));
        }
    }

    @Test
    void headReturnGenerationBoundariesRestoreOnlyTheirOuterScope() {
        GeometryGeneration.begin();
        try {
            final TRSRTransformation outer = OwnedModelTransforms.create(matrix());
            GeometryGeneration.begin();
            assertNotSame(outer, OwnedModelTransforms.create(matrix()));
            GeometryGeneration.endCurrent();
            assertSame(outer, OwnedModelTransforms.create(matrix()));
            GeometryGeneration.endCurrent();
            assertNotSame(outer, OwnedModelTransforms.create(matrix()));
        } finally {
            GeometryGeneration.endCurrent();
        }
    }

    @Test
    void fullMapPoolStillSharesPreviouslyCapturedMaps() {
        try (GeometryGeneration ignored = GeometryGeneration.begin()) {
            final TRSRTransformation[] transforms = new TRSRTransformation[4];
            for (int i = 0; i < transforms.length; i++) {
                final Matrix4f value = matrix();
                value.m03 = i;
                transforms[i] = OwnedModelTransforms.create(value);
            }
            final TransformType[] types = TransformType.values();
            ImmutableMap<TransformType, TRSRTransformation> first = null;
            for (int i = 0; i < 16384; i++) {
                final ImmutableMap.Builder<TransformType, TRSRTransformation> builder = ImmutableMap.builder();
                for (int type = 0; type < 7; type++) builder.put(types[type], transforms[(i >>> (type * 2)) & 3]);
                final ImmutableMap<TransformType, TRSRTransformation> value = builder.build();
                assertSame(value, OwnedModelTransforms.share(value));
                if (first == null) first = value;
            }
            final ImmutableMap.Builder<TransformType, TRSRTransformation> duplicate = ImmutableMap.builder();
            duplicate.putAll(first);
            assertSame(first, OwnedModelTransforms.share(duplicate.build()), "A full pool must still reuse existing maps");
            final ImmutableMap<TransformType, TRSRTransformation> newMap = ImmutableMap.of(types[7], transforms[0]);
            assertSame(newMap, OwnedModelTransforms.share(newMap), "New entries still obey the pool bound");
        }
    }
}
