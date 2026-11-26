package github.kasuminova.novaeng.client.render;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DirectionalOffsetCacheTest {
    @Test
    void reusesCoordinatesAcrossDifferentInputsAndOperations() {
        final DirectionalOffsetCache cache = new DirectionalOffsetCache();
        final BlockPos first = cache.offset(new BlockPos(10, 20, 30), EnumFacing.EAST);
        assertSame(first, cache.add(new BlockPos(9, 20, 30), new Vec3i(2, 0, 0)));
        cache.offset(new BlockPos(-100, -100, -100), EnumFacing.DOWN);
        assertSame(first, cache.offset(new BlockPos(10, 20, 30), EnumFacing.EAST));
    }

    @Test
    void observesMutableInputChangesAndPublishesImmutableResults() {
        final DirectionalOffsetCache cache = new DirectionalOffsetCache();
        final BlockPos.MutableBlockPos input = new BlockPos.MutableBlockPos(1, 2, 3);
        final BlockPos retained = cache.offset(input, EnumFacing.UP);
        input.setPos(7, 8, 9);
        assertEquals(new BlockPos(7, 9, 9), cache.offset(input, EnumFacing.UP));
        assertEquals(new BlockPos(1, 3, 3), retained);
        assertEquals(BlockPos.class, retained.getClass());
        assertNotSame(input, retained);
        for (int i = 0; i < 4096; i++) cache.offset(new BlockPos(i, i, i), EnumFacing.NORTH);
        assertEquals(new BlockPos(1, 3, 3), retained);
    }

    @Test
    void preservesZeroOffsetIdentityAndSubclassMethods() {
        final DirectionalOffsetCache cache = new DirectionalOffsetCache();
        final BlockPos input = new BlockPos(1, 2, 3);
        assertSame(input, cache.add(input, new Vec3i(0, 0, 0)));
        final BlockPos unusual = new BlockPos(1, 2, 3) {
            @Override
            public BlockPos add(final Vec3i offset) {
                return input;
            }

            @Override
            public BlockPos offset(final EnumFacing facing) {
                return input;
            }
        };
        cache.offset(input, EnumFacing.EAST);
        assertSame(input, cache.add(unusual, new Vec3i(1, 0, 0)));
        assertSame(input, cache.offset(unusual, EnumFacing.EAST));
    }

    @Test
    void matchesVanillaAcrossEvictionsOverflowAndFullWidthCoordinates() {
        final DirectionalOffsetCache cache = new DirectionalOffsetCache();
        final Random random = new Random(0x43544d);
        final EnumFacing[] facings = EnumFacing.values();
        for (int i = 0; i < 10000; i++) {
            final BlockPos input = new BlockPos(random.nextInt(), random.nextInt(), random.nextInt());
            final Vec3i offset = new Vec3i(random.nextInt(), random.nextInt(), random.nextInt());
            final EnumFacing facing = facings[i % facings.length];
            assertEquals(input.add(offset), cache.add(input, offset));
            assertEquals(input.offset(facing), cache.offset(input, facing));
        }
        final BlockPos edge = new BlockPos(Integer.MAX_VALUE, Integer.MIN_VALUE, 0);
        assertEquals(edge.offset(EnumFacing.EAST), cache.offset(edge, EnumFacing.EAST));
        assertEquals(edge.offset(EnumFacing.DOWN), cache.offset(edge, EnumFacing.DOWN));
        assertEquals(new BlockPos(1 << 26, 1, 0), cache.offset(new BlockPos(1 << 26, 0, 0), EnumFacing.UP));
        assertEquals(new BlockPos(0, 1, 0), cache.offset(BlockPos.ORIGIN, EnumFacing.UP));
    }
}
