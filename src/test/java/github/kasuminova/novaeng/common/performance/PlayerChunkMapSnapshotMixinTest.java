package github.kasuminova.novaeng.common.performance;

import com.google.common.base.Predicate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerChunkMapSnapshotMixinTest {

    private static Iterator<?> iterator(final Class<?> type, final Object instance) throws Exception {
        return (Iterator<?>) type.getMethod("getChunkIterator").invoke(instance);
    }

    private static WorldServer world() throws Exception {
        final DedicatedServer server = WorldgenTestBlocks.allocate(DedicatedServer.class);
        server.setPlayerList(WorldgenTestBlocks.allocate(DedicatedPlayerList.class));
        final WorldServer world = WorldgenTestBlocks.allocate(WorldServer.class);
        final Field field = WorldServer.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(world, server);
        return world;
    }

    private static TestChunk chunk(final boolean light, final boolean terrain, final boolean ticked) throws Exception {
        final TestChunk chunk = WorldgenTestBlocks.allocate(TestChunk.class);
        chunk.light = light;
        chunk.terrain = terrain;
        chunk.ticked = ticked;
        return chunk;
    }

    private static TestEntry entry(@Nullable final Chunk chunk, final boolean nearPlayer) throws Exception {
        final TestEntry entry = WorldgenTestBlocks.allocate(TestEntry.class);
        entry.chunk = chunk;
        entry.nearPlayer = nearPlayer;
        return entry;
    }

    @Test
    void actualPlayerChunkMapConstructorCreatesIndependentPoolsWithBothCompanionMixins() throws Exception {
        final Class<?> type = new PerformanceMixinTestLoader().loadClass("net.minecraft.server.management.PlayerChunkMap");
        final Object first = type.getConstructor(WorldServer.class).newInstance(world());
        final Object second = type.getConstructor(WorldServer.class).newInstance(world());
        final Field storage = type.getDeclaredField("nova$chunkSnapshots");
        storage.setAccessible(true);
        assertInstanceOf(TickSnapshotPool.class, storage.get(first));
        assertInstanceOf(TickSnapshotPool.class, storage.get(second));
        assertNotSame(storage.get(first), storage.get(second));
        final Field other = type.getDeclaredField("novaeng$chunkCaches");
        other.setAccessible(true);
        assertNotNull(other.get(first));
        assertFalse(iterator(type, first).hasNext());
        assertFalse(iterator(type, second).hasNext());
    }

    @Test
    void realCombinedMixinsUseTheSnapshotPoolAndKeepOutstandingSnapshotsIndependent() throws Exception {
        final Class<?> type = new PerformanceMixinTestLoader().loadClass("net.minecraft.server.management.PlayerChunkMap");
        final Object instance = type.getConstructor(WorldServer.class).newInstance(world());
        final List<PlayerChunkMapEntry> entries = new ObjectArrayList<>();
        entries.add(entry(null, false));
        final TestChunk unlit = chunk(false, true, true);
        final TestChunk unticked = chunk(true, true, false);
        final TestChunk nearPlayer = chunk(true, true, true);
        entries.add(entry(unlit, false));
        entries.add(entry(unticked, false));
        entries.add(entry(nearPlayer, true));
        entries.add(entry(chunk(true, true, true), false));
        entries.add(entry(chunk(false, false, true), false));
        final Field field = type.getDeclaredField("entries");
        field.setAccessible(true);
        field.set(instance, entries);
        final Iterator<?> first = iterator(type, instance);
        assertEquals(TickSnapshotPool.class, first.getClass().getEnclosingClass(),
            "StellarCore's HEAD injector must not replace the pool with its old stream snapshot");
        assertSame(unlit, first.next());
        entries.clear();
        final TestChunk nested = chunk(false, true, true);
        entries.add(entry(nested, false));
        final Iterator<?> second = iterator(type, instance);
        assertSame(nested, second.next());
        assertFalse(second.hasNext());
        assertSame(unticked, first.next());
        assertSame(nearPlayer, first.next());
        assertFalse(first.hasNext());
        final Iterator<?> reused = iterator(type, instance);
        assertSame(nested, reused.next());
        assertFalse(reused.hasNext());
    }

    @Test
    void otherNewInstanceStateIsAssignedInTheActualTransformedTargets() throws Exception {
        final Class<?> spawner = new PerformanceMixinTestLoader().loadClass("net.minecraft.world.WorldEntitySpawner");
        final Object instance = spawner.getConstructor().newInstance();
        for (final String name : new String[]{"nova$candidates", "nova$shuffleRandom"}) {
            final Field field = spawner.getDeclaredField(name);
            field.setAccessible(true);
            assertNotNull(field.get(instance), name);
        }
        final ClassNode stone = new ClassNode();
        new ClassReader(PerformanceMixinTestLoader.transformed("net.minecraft.block.BlockStone")).accept(stone, 0);
        assertTrue(stone.methods.stream().filter(method -> method.name.equals("<init>"))
                                .anyMatch(method -> Stream.of(method.instructions.toArray()).anyMatch(instruction ->
                                    instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                                        && field.name.equals("nova$metadataStates"))));
    }

    @Test
    void realConstructorCallsTheExplicitPoolInitializerBeforeReturning() throws Exception {
        final ClassNode target = new ClassNode();
        new ClassReader(PerformanceMixinTestLoader.transformed("net.minecraft.server.management.PlayerChunkMap")).accept(target, 0);
        final MethodNode constructor = target.methods.stream().filter(method -> method.name.equals("<init>")
            && method.desc.equals("(Lnet/minecraft/world/WorldServer;)V")).findFirst().orElseThrow();
        boolean initialized = false;
        for (final var instruction : constructor.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(target.name)
                && call.name.contains("initializeSnapshotPool")) {
                initialized = true;
            }
            if (instruction.getOpcode() == Opcodes.RETURN) {
                assertTrue(initialized, "Every successful constructor exit must initialize the pool");
            }
        }
    }

    private static final class TestEntry extends PlayerChunkMapEntry {
        private Chunk chunk;
        private boolean nearPlayer;

        private TestEntry() {
            super(null, 0, 0);
        }

        @Override
        @Nullable
        public Chunk getChunk() {
            return this.chunk;
        }

        @Override
        public boolean hasPlayerMatchingInRange(final double range, @Nonnull final Predicate<EntityPlayerMP> predicate) {
            return this.nearPlayer;
        }
    }

    private static final class TestChunk extends Chunk {
        private boolean light;
        private boolean terrain;
        private boolean ticked;

        private TestChunk() {
            super(null, 0, 0);
        }

        @Override
        public boolean isLightPopulated() {
            return this.light;
        }

        @Override
        public boolean isTerrainPopulated() {
            return this.terrain;
        }

        @Override
        public boolean wasTicked() {
            return this.ticked;
        }
    }
}
