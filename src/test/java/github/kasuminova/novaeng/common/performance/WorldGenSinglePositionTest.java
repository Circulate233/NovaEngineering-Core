package github.kasuminova.novaeng.common.performance;

import com.google.common.base.Predicate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WorldGenSinglePositionTest {

    static final Block SOURCE = block("source");
    static final Block ORE_A = block("ore-a");
    static final Block ORE_B = block("ore-b");

    static Placement placement(final boolean optimized) throws Exception {
        final ClassLoader loader = new WorldgenFixtureLoader(optimized);
        final Class<?> material = loader.loadClass("cofh.cofhworld.util.random.WeightedBlock");
        final Class<?> generator = loader.loadClass("cofh.cofhworld.world.generator.WorldGen");
        final Object materials = Array.newInstance(material, 1);
        Array.set(materials, 0, material.getConstructor(Block.class, int.class).newInstance(SOURCE, -1));
        final List<Object> ores = new ObjectArrayList<>();
        ores.add(material.getConstructor(Block.class).newInstance(ORE_A));
        ores.add(material.getConstructor(Block.class).newInstance(ORE_B));
        return new Placement(generator.getMethod("generateBlock", World.class, Random.class, int.class, int.class,
            int.class, materials.getClass(), List.class), materials, ores);
    }

    static RecordingWorld world() throws Exception {
        final Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        final RecordingWorld world = (RecordingWorld) ((Unsafe) field.get(null)).allocateInstance(RecordingWorld.class);
        world.events = new ObjectArrayList<>();
        world.retainedPositions = new ObjectArrayList<>();
        world.replaceable = true;
        world.writeSuccess = true;
        world.recording = true;
        return world;
    }

    private static Block block(final String name) {
        try {
            final TestBlock block = WorldgenTestBlocks.allocate(TestBlock.class);
            block.name = name;
            return WorldgenTestBlocks.states(block);
        } catch (final Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Test
    void actualPlacementRetainsChecksWritesRandomSequenceAndImmutableCoordinates() throws Exception {
        final Placement before = placement(false);
        final Placement after = placement(true);
        final Random input = new Random(0xC0F4L);
        for (int iteration = 0; iteration < 300; iteration++) {
            final RecordingWorld oldWorld = world();
            final RecordingWorld newWorld = world();
            oldWorld.replaceable = newWorld.replaceable = iteration % 3 != 0;
            oldWorld.writeSuccess = newWorld.writeSuccess = iteration % 5 != 0;
            final int x = input.nextInt();
            final int y = input.nextInt(400) - 40;
            final int z = input.nextInt();
            final Random oldRandom = new Random(iteration);
            final Random newRandom = new Random(iteration);
            final Object oldMaterials = iteration % 4 == 0 ? null : before.materials();
            final Object newMaterials = iteration % 4 == 0 ? null : after.materials();
            final Object oldResult = before.method().invoke(null, oldWorld, oldRandom, x, y, z, oldMaterials, before.ores());
            final Object newResult = after.method().invoke(null, newWorld, newRandom, x, y, z, newMaterials, after.ores());
            assertEquals(oldResult, newResult);
            assertEquals(oldWorld.events, newWorld.events);
            assertEquals(oldRandom.nextLong(), newRandom.nextLong());
            for (final BlockPos position : newWorld.retainedPositions) {
                assertEquals(new BlockPos(x, y, z), position);
                assertEquals(BlockPos.class, position.getClass());
            }
            if (oldMaterials != null && oldWorld.replaceable && !newWorld.events.isEmpty()) {
                assertSame(newWorld.retainedPositions.getFirst(), newWorld.retainedPositions.getLast());
            }
        }
    }

    static final class RecordingWorld extends World {
        List<String> events;
        List<BlockPos> retainedPositions;
        boolean replaceable;
        boolean writeSuccess;
        boolean recording;
        long checksum;
        int placements;

        private RecordingWorld() {
            super(null, null, null, null, false);
            throw new AssertionError("Only overridden read/write behavior is used");
        }

        @Override
        public IBlockState getBlockState(final BlockPos position) {
            if (this.recording) {
                this.events.add("read:" + position.getX() + ":" + position.getY() + ":" + position.getZ());
                this.retainedPositions.add(position);
            }
            this.checksum = this.checksum * 31 + position.toLong();
            return SOURCE.getDefaultState();
        }

        @Override
        public boolean setBlockState(final BlockPos position, final IBlockState state, final int flags) {
            if (this.recording) {
                this.events.add("write:" + ((TestBlock) state.getBlock()).name + ":" + flags);
                this.retainedPositions.add(position);
            }
            this.placements++;
            this.checksum = this.checksum * 31 + ((TestBlock) state.getBlock()).name.hashCode();
            return this.writeSuccess;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            throw new AssertionError();
        }

        @Override
        protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) {
            throw new AssertionError();
        }
    }

    private static final class TestBlock extends Block {
        private String name;

        private TestBlock(final String name) {
            super(Material.ROCK);
            this.name = name;
        }

        @Override
        public boolean isReplaceableOreGen(final IBlockState state, final IBlockAccess world, final BlockPos position,
                                           final Predicate<IBlockState> predicate) {
            final RecordingWorld recording = (RecordingWorld) world;
            if (recording.recording) {
                recording.events.add("replace");
            }
            return recording.replaceable && predicate.apply(state);
        }
    }

    record Placement(Method method, Object materials, List<Object> ores) {
    }
}
