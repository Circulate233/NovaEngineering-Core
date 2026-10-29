package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class OreVeinGeometryTest {

    static GeneratorFactory factory(final boolean optimized) throws Exception {
        final ClassLoader loader = new WorldgenFixtureLoader(optimized);
        final Class<?> material = loader.loadClass("cofh.cofhworld.util.random.WeightedBlock");
        final Class<?> generator = loader.loadClass("cofh.cofhworld.world.generator.WorldGenMinableCluster");
        final List<Object> materials = new ObjectArrayList<>();
        materials.add(material.getConstructor(Block.class, int.class)
                              .newInstance(WorldGenSinglePositionTest.SOURCE, -1));
        final List<Object> ores = new ObjectArrayList<>();
        ores.add(material.getConstructor(Block.class).newInstance(WorldGenSinglePositionTest.ORE_A));
        ores.add(material.getConstructor(Block.class).newInstance(WorldGenSinglePositionTest.ORE_B));
        final Method generate = generator.getMethod("generate", World.class, Random.class, BlockPos.class);
        return size -> new Generator(generator.getConstructor(List.class, int.class, List.class).newInstance(ores, size, materials), generate);
    }

    @Test
    void actualGeneratorsRetainEveryVisitRepeatedWriteResultAndRandomDraw() throws Exception {
        final Random inputs = new Random(0xC0F4B10CL);
        final GeneratorFactory original = factory(false);
        final GeneratorFactory optimized = factory(true);
        final int[] boundaries = {-30_000_000, 30_000_000, -1, 0, 4_194_303, 4_194_304,
            8_388_607, 8_388_608, 8_388_609, 16_777_215, 16_777_216, 16_777_217,
            -8_388_607, -8_388_608, -8_388_609, -16_777_215, -16_777_216, -16_777_217};
        for (int size = 0; size <= 45; size++) {
            final Generator before = original.create(size);
            final Generator after = optimized.create(size);
            for (int trial = 0; trial < 32; trial++) {
                final int x = trial < boundaries.length ? boundaries[trial] : inputs.nextInt(200000) - 100000;
                final int z = -x;
                final BlockPos position = new BlockPos(x, inputs.nextInt(300) - 20, z);
                final long seed = inputs.nextLong();
                final Random oldRandom = new Random(seed);
                final Random newRandom = new Random(seed);
                final WorldGenSinglePositionTest.RecordingWorld oldWorld = WorldGenSinglePositionTest.world();
                final WorldGenSinglePositionTest.RecordingWorld newWorld = WorldGenSinglePositionTest.world();
                oldWorld.replaceable = newWorld.replaceable = trial % 3 != 0;
                oldWorld.writeSuccess = newWorld.writeSuccess = trial % 5 != 0;
                final Object expected = before.generate.invoke(before.instance, oldWorld, oldRandom, position);
                final Object actual = after.generate.invoke(after.instance, newWorld, newRandom, position);
                final String context = "size=" + size + ", trial=" + trial;
                assertEquals(expected, actual, context);
                assertEquals(oldWorld.events, newWorld.events, context);
                assertEquals(oldWorld.retainedPositions, newWorld.retainedPositions, context);
                assertEquals(oldWorld.checksum, newWorld.checksum, context);
                assertEquals(oldRandom.nextLong(), newRandom.nextLong(), context);
            }
        }
    }

    @Test
    void scratchBuffersSupportNestedGenerationAndBoundedReuse() {
        final float[] first = OreGeometryScratch.borrow();
        final float[] nested = OreGeometryScratch.borrow();
        assertNotSame(first, nested);
        first[0] = 42;
        OreGeometryScratch.release(nested);
        assertSame(nested, OreGeometryScratch.borrow());
        assertEquals(42, first[0]);
        OreGeometryScratch.release(nested);
        OreGeometryScratch.release(first);
        assertSame(first, OreGeometryScratch.borrow());
        OreGeometryScratch.release(first);
    }

    @FunctionalInterface
    interface GeneratorFactory {
        Generator create(int size) throws Exception;
    }

    record Generator(Object instance, Method generate) {
    }
}
