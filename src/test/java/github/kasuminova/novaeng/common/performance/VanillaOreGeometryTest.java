package github.kasuminova.novaeng.common.performance;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaOreGeometryTest {

    private static void compare(final Generator before, final Generator after, final BlockPos position,
                                final long seed, final int trial) throws Exception {
        final WorldGenSinglePositionTest.RecordingWorld oldWorld = WorldGenSinglePositionTest.world();
        final WorldGenSinglePositionTest.RecordingWorld newWorld = WorldGenSinglePositionTest.world();
        oldWorld.replaceable = newWorld.replaceable = trial % 3 != 0;
        oldWorld.writeSuccess = newWorld.writeSuccess = trial % 5 != 0;
        oldWorld.recording = newWorld.recording = trial < 100;
        final Random oldRandom = new Random(seed);
        final Random newRandom = new Random(seed);
        final String context = "position=" + position + ", seed=" + seed;
        assertEquals(before.generate.invoke(before.instance, oldWorld, oldRandom, position),
            after.generate.invoke(after.instance, newWorld, newRandom, position), context);
        assertEquals(oldWorld.events, newWorld.events, context);
        assertEquals(oldWorld.retainedPositions, newWorld.retainedPositions, context);
        assertEquals(oldWorld.checksum, newWorld.checksum, context);
        assertEquals(oldWorld.placements, newWorld.placements, context);
        assertEquals(oldRandom.nextLong(), newRandom.nextLong(), context);
    }

    static Factory factory(final boolean optimized) throws Exception {
        final Class<?> type = new WorldgenFixtureLoader(optimized).loadClass("net.minecraft.world.gen.feature.WorldGenMinable");
        final Constructor<?> constructor = type.getConstructor(IBlockState.class, int.class, Predicate.class);
        final Method generate = type.getMethod("generate", World.class, Random.class, BlockPos.class);
        return (size, predicate) -> new Generator(constructor.newInstance(WorldGenSinglePositionTest.ORE_A.getDefaultState(), size, predicate), generate);
    }

    @Test
    void actualGeneratorsPreserveEveryVisitWritePredicateAndRandomDraw() throws Exception {
        final Factory before = factory(false);
        final Factory after = factory(true);
        final int[] coordinates = {-30_000_000, 30_000_000, -1, 0, 4_194_303, 4_194_304,
            8_388_607, 8_388_608, 8_388_609, 16_777_215, 16_777_216, 16_777_217};
        final Random inputs = new Random(0x0DEB10CL);
        for (int size = -1; size <= 65; size++) {
            final Generator oldGenerator = before.create(size, state -> true);
            final Generator newGenerator = after.create(size, state -> true);
            for (int trial = 0; trial < 20; trial++) {
                final int x = trial < coordinates.length ? coordinates[trial] : inputs.nextInt(200_000) - 100_000;
                compare(oldGenerator, newGenerator, new BlockPos(x, inputs.nextInt(300) - 20, -x), inputs.nextLong(), trial);
            }
        }
        for (final int size : new int[]{128, 256}) {
            compare(before.create(size, state -> true), after.create(size, state -> true),
                new BlockPos(1000, 32, -1000), 0xABCDL, 101);
        }
    }

    @Test
    void predicatesCanChangeLiveGeneratorFieldsAndExceptionsReleaseScratch() throws Exception {
        WorldGenSinglePositionTest.RecordingWorld originalWorld = null;
        Long originalRandom = null;
        for (final boolean optimized : new boolean[]{false, true}) {
            final Factory factory = factory(optimized);
            final Generator[] generator = new Generator[1];
            generator[0] = factory.create(32, state -> {
                try {
                    generator[0].instance.getClass().getField("numberOfBlocks").setInt(generator[0].instance, 4);
                    generator[0].instance.getClass().getField("oreBlock").set(generator[0].instance,
                        WorldGenSinglePositionTest.ORE_B.getDefaultState());
                } catch (final ReflectiveOperationException failure) {
                    throw new AssertionError(failure);
                }
                return true;
            });
            final WorldGenSinglePositionTest.RecordingWorld world = WorldGenSinglePositionTest.world();
            final Random random = new Random(42);
            assertEquals(true, generator[0].generate.invoke(generator[0].instance, world, random, new BlockPos(0, 32, 0)));
            assertTrue(world.events.stream().anyMatch(event -> event.equals("write:ore-b:2")));
            assertEquals(4, generator[0].instance.getClass().getField("numberOfBlocks").getInt(generator[0].instance));
            if (optimized) {
                assertNotNull(originalWorld);
                assertNotNull(originalRandom);
                assertEquals(originalWorld.events, world.events);
                assertEquals(originalRandom.longValue(), random.nextLong());
            } else {
                originalWorld = world;
                originalRandom = random.nextLong();
            }
        }
        final double[] scratch = VanillaOreGeometryScratch.borrow();
        VanillaOreGeometryScratch.release(scratch);
        final Generator throwing = factory(true).create(32, state -> {
            throw new IllegalStateException("predicate");
        });
        assertThrows(InvocationTargetException.class, () -> throwing.generate.invoke(throwing.instance,
            WorldGenSinglePositionTest.world(), new Random(42), new BlockPos(0, 32, 0)));
        assertSame(scratch, VanillaOreGeometryScratch.borrow());
        VanillaOreGeometryScratch.release(scratch);
    }

    @Test
    void doubleScratchSupportsNestedOwnershipAndBoundedReuse() {
        final double[] outer = VanillaOreGeometryScratch.borrow();
        final double[] inner = VanillaOreGeometryScratch.borrow();
        assertNotSame(outer, inner);
        outer[0] = 42;
        VanillaOreGeometryScratch.release(inner);
        assertSame(inner, VanillaOreGeometryScratch.borrow());
        assertEquals(42, outer[0]);
        VanillaOreGeometryScratch.release(inner);
        VanillaOreGeometryScratch.release(outer);
    }

    @FunctionalInterface
    interface Factory {
        Generator create(int size, Predicate<IBlockState> predicate) throws Exception;
    }

    record Generator(Object instance, Method generate) {
    }
}
