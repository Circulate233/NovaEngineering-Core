package github.kasuminova.novaeng.client.moreoverlays;

import at.feldim2425.moreoverlays.api.lightoverlay.ILightScanner;
import at.feldim2425.moreoverlays.config.Config;
import at.feldim2425.moreoverlays.lightoverlay.LightScannerVanilla;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightOverlayAllocationTest {
    private static ScanWorld world() throws Exception {
        final ScanWorld world = allocate(ScanWorld.class);
        world.calls = new ObjectArrayList<>();
        final SpawnBlock block = allocate(SpawnBlock.class);
        world.normal = state(block, true);
        world.open = state(block, false);
        return world;
    }

    private static IBlockState state(final Block block, final boolean normal) {
        return (IBlockState) Proxy.newProxyInstance(IBlockState.class.getClassLoader(), new Class<?>[]{IBlockState.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getBlock" -> block;
                case "isNormalCube" -> normal;
                default -> throw new AssertionError("Unexpected state query: " + method);
            });
    }

    private static <T> T allocate(final Class<T> type) throws Exception {
        final Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        final var field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static String key(final BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    @Test
    void allVanillaSpawnBranchesKeepResultsAndWorldQueryOrder() throws Exception {
        final ScanWorld world = world();
        final LightScanCache cache = new LightScanCache();
        final LightScannerVanilla original = new LightScannerVanilla();
        final Settings saved = new Settings();
        try {
            Config.light_SaveLevel = 8;
            for (final boolean ignoreLayer : new boolean[]{false, true}) {
                Config.light_IgnoreLayer = ignoreLayer;
                for (int x = -22; x < 22; x++) {
                    final BlockPos pos = new BlockPos(x, 64, -8);
                    world.calls.clear();
                    final byte expected = original.getSpawnModeAt(pos, world);
                    final List<String> expectedCalls = new ObjectArrayList<>(world.calls);
                    world.calls.clear();
                    assertEquals(expected, cache.spawnMode(pos, world), "spawn mode at " + pos);
                    assertEquals(expectedCalls, world.calls, "queries at " + pos + ", ignoreLayer=" + ignoreLayer);
                }
            }
        } finally {
            saved.restore();
        }
    }

    @Test
    void scanMatchesActualModAndPublicListRemainsLiveAfterFirstExposure() throws Exception {
        final Settings saved = new Settings();
        try {
            Config.light_SaveLevel = 8;
            Config.light_HRange = 6;
            Config.light_UpRange = 1;
            Config.light_DownRange = 2;
            Config.light_IgnoreLayer = false;
            Config.light_IgnoreSpawn = true;
            final ScanWorld world = world();
            final Player player = allocate(Player.class);
            player.world = world;
            player.posX = -0.25;
            player.posY = 64.75;
            player.posZ = -10.75;
            final LightScannerVanilla original = new LightScannerVanilla();
            final LightScanCache cache = new LightScanCache();
            final List<Pair<BlockPos, Byte>> publicModes = new ObjectArrayList<>();

            original.update(player);
            final List<Pair<BlockPos, Byte>> expected = original.getLightModes();
            final List<String> expectedCalls = new ObjectArrayList<>(world.calls);
            world.calls.clear();
            cache.update(original, player, publicModes);
            assertEquals(expectedCalls, world.calls);
            assertFalse(expected.isEmpty());
            assertEquals(expected.size(), cache.size());
            assertTrue(publicModes.isEmpty(), "ordinary scan must not allocate public Pair/BlockPos results");
            assertFalse(cache.isExposed());
            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i).getKey(), new BlockPos(cache.x(i), cache.y(i), cache.z(i)));
                assertEquals(expected.get(i).getValue().byteValue(), cache.mode(i));
            }
            assertSame(publicModes, cache.expose(publicModes));
            assertEquals(expected, publicModes);
            final var storage = LightScanCache.class.getDeclaredField("coordinates");
            storage.setAccessible(true);
            assertNull(storage.get(cache), "the public list replaces primitive storage once exposed");
            final Pair<BlockPos, Byte> first = publicModes.get(0);
            assertSame(first, cache.expose(publicModes).get(0));
            publicModes.clear();
            publicModes.add(Pair.of(new BlockPos(9, 10, 11), (byte) 2));
            assertEquals(1, cache.expose(publicModes).size(), "external list edits remain visible");
            for (int repeat = 0; repeat < 3; repeat++) {
                cache.update(original, player, publicModes);
                assertSame(publicModes, cache.expose(publicModes));
                assertEquals(expected, publicModes, "the held public list is cleared and refilled after an update");
                assertNull(storage.get(cache));
            }
            cache.clear();
            publicModes.clear(); // The original clear method, retained by the mixin.
            assertEquals(0, cache.size());
            assertSame(publicModes, cache.expose(publicModes));
            assertTrue(publicModes.isEmpty());
            assertTrue(cache.isExposed(), "later updates must continue maintaining already-held list references");
            assertTrue(LightScanCache.supports(original));
            assertFalse(LightScanCache.supports(new LightScannerVanilla() {
            }), "extension scanners must retain original semantics");
        } finally {
            saved.restore();
        }
    }

    @Test
    void primitiveBatchPreservesFullCoordinatesAndMatchesExposedInterleavedModes() {
        final CacheScanner scanner = new CacheScanner();
        for (int i = 0; i < 130; i++) {
            scanner.cache.add(Integer.MIN_VALUE + i, -5000 + i, Integer.MAX_VALUE - i, (byte) (i % 2 + 1));
        }
        scanner.cache.add(1, 2, 3, (byte) 0);
        scanner.cache.add(4, 5, 6, (byte) 9);
        final RecordingSink primitive = new RecordingSink();
        LightOverlayBatch.emit(scanner, 0x9a1537, 0x02c1ef, primitive);
        assertFalse(scanner.cache.isExposed());
        assertTrue(scanner.modes.isEmpty());
        assertEquals(1, primitive.begins);
        assertEquals(1, primitive.ends);
        assertEquals(520, primitive.vertices.size());
        assertEquals(new Vertex(Integer.MIN_VALUE, -4999.995, Integer.MAX_VALUE,
            2 / 255.0f, 193 / 255.0f, 239 / 255.0f), primitive.vertices.get(0));
        assertEquals(new Vertex((double) Integer.MIN_VALUE + 1, -4999.995, (double) Integer.MAX_VALUE + 1,
            2 / 255.0f, 193 / 255.0f, 239 / 255.0f), primitive.vertices.get(1));
        assertEquals(154 / 255.0f, primitive.vertices.get(4).red);
        final RecordingSink exposed = new RecordingSink();
        scanner.getLightModes();
        LightOverlayBatch.emit(scanner, 0x9a1537, 0x02c1ef, exposed);
        assertEquals(primitive.vertices, exposed.vertices);
        scanner.modes.clear();
        final RecordingSink edited = new RecordingSink();
        LightOverlayBatch.emit(scanner, 0x9a1537, 0x02c1ef, edited);
        assertEquals(0, edited.begins, "exposed public list edits take precedence over internal arrays");
        assertEquals(0, edited.ends);
    }

    @Test
    void customScannerSkipsNullAndInvalidModesBeforeReadingPositions() {
        final List<Pair<BlockPos, Byte>> entries = new ObjectArrayList<>();
        entries.add(Pair.of(null, null));
        entries.add(Pair.of(null, (byte) 0));
        entries.add(Pair.of(null, (byte) -1));
        entries.add(Pair.of(new BlockPos(-3, 63, 7), (byte) 2));
        entries.add(Pair.of(new BlockPos(8, -20, 1), (byte) 1));
        final RecordingSink result = new RecordingSink();
        LightOverlayBatch.emit(new ListScanner(entries), 0xff0000, 0x00ff00, result);
        assertEquals(1, result.begins);
        assertEquals(1, result.ends);
        assertEquals(8, result.vertices.size());
        assertEquals(new Vertex(-3, 63.005, 7, 1, 0, 0), result.vertices.get(0));
        assertEquals(new Vertex(8, -19.995, 1, 0, 1, 0), result.vertices.get(4));
        entries.clear();
        entries.add(Pair.of(null, (byte) 0));
        final RecordingSink empty = new RecordingSink();
        LightOverlayBatch.emit(new ListScanner(entries), 0, 0, empty);
        assertEquals(0, empty.begins);
        assertEquals(0, empty.ends);
        entries.add(Pair.of(null, (byte) 1));
        assertThrows(NullPointerException.class, () -> LightOverlayBatch.emit(new ListScanner(entries), 0, 0, empty));
    }

    @Test
    void customListFailureClosesStartedBatchAndPropagatesOriginalFailure() {
        final IllegalStateException failure = new IllegalStateException("custom scanner");
        final List<Pair<BlockPos, Byte>> entries = new AbstractList<>() {
            @Override
            public Pair<BlockPos, Byte> get(final int index) {
                if (index == 1) throw failure;
                return Pair.of(new BlockPos(1, 2, 3), (byte) 1);
            }

            @Override
            public int size() {
                return 2;
            }
        };
        final RecordingSink sink = new RecordingSink();
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> LightOverlayBatch.emit(new ListScanner(entries), 0, 0, sink)));
        assertEquals(1, sink.begins);
        assertEquals(1, sink.ends);
        assertEquals(4, sink.vertices.size());
    }

    private static final class ScanWorld extends World {
        List<String> calls;
        IBlockState normal, open;

        private ScanWorld() {
            super(null, null, null, null, false);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) {
            return false;
        }

        @Override
        public int getLightFor(final EnumSkyBlock type, final BlockPos pos) {
            calls.add(type + ":" + key(pos));
            final int scenario = Math.floorMod(pos.getX(), 11);
            return type == EnumSkyBlock.BLOCK ? (scenario == 0 ? 8 : 0) : (scenario == 9 ? 8 : 0);
        }

        @Override
        public IBlockState getBlockState(final BlockPos pos) {
            calls.add("state:" + key(pos));
            final int scenario = Math.floorMod(pos.getX(), 11);
            return (scenario == 2 && pos.getY() == 64 || scenario == 3 && pos.getY() == 65) ? normal : open;
        }

        @Override
        public boolean isAirBlock(final BlockPos pos) {
            calls.add("air:" + key(pos));
            return Math.floorMod(pos.getX(), 11) == 4;
        }

        @Override
        public List<AxisAlignedBB> getCollisionBoxes(final Entity entity, final AxisAlignedBB bb) {
            calls.add("collision:" + bb);
            final int scenario = Math.floorMod((int) Math.floor(bb.minX), 11);
            return scenario == 5 && bb.minY == 64 || scenario == 7 && bb.minY == 65
                ? Collections.singletonList(bb) : Collections.emptyList();
        }

        @Override
        public boolean containsAnyLiquid(final AxisAlignedBB bb) {
            calls.add("liquid:" + bb);
            final int scenario = Math.floorMod((int) Math.floor(bb.minX), 11);
            return scenario == 6 && bb.minY == 64 || scenario == 8 && bb.minY == 65;
        }
    }

    private static final class SpawnBlock extends Block {
        private SpawnBlock() {
            super(Material.ROCK);
        }

        @Override
        public boolean canCreatureSpawn(final IBlockState state, final IBlockAccess access, final BlockPos pos,
                                        final EntityLiving.SpawnPlacementType type) {
            ((ScanWorld) access).calls.add("spawn:" + key(pos));
            return Math.floorMod(pos.getX(), 11) != 1;
        }
    }

    private static final class Player extends EntityPlayer {
        private Player() {
            super(null, null);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }

    private static class ListScanner implements ILightScanner {
        final List<Pair<BlockPos, Byte>> modes;

        ListScanner(final List<Pair<BlockPos, Byte>> modes) {
            this.modes = modes;
        }

        @Override
        public void update(final EntityPlayer player) {
        }

        @Override
        public void clear() {
            modes.clear();
        }

        @Override
        public List<Pair<BlockPos, Byte>> getLightModes() {
            return modes;
        }
    }

    private static final class CacheScanner extends ListScanner implements PrimitiveLightScanner {
        final LightScanCache cache = new LightScanCache();

        CacheScanner() {
            super(new ObjectArrayList<>());
        }

        @Override
        public LightScanCache novaeng$primitiveLightModes() {
            return cache;
        }

        @Override
        public List<Pair<BlockPos, Byte>> getLightModes() {
            return cache.expose(modes);
        }
    }

    private record Vertex(double x, double y, double z, float red, float green, float blue) {
    }

    private static final class RecordingSink implements LightOverlayBatch.Sink {
        final List<Vertex> vertices = new ObjectArrayList<>();
        int begins, ends;

        @Override
        public void begin() {
            begins++;
        }

        @Override
        public void vertex(final double x, final double y, final double z, final float r, final float g, final float b) {
            vertices.add(new Vertex(x, y, z, r, g, b));
        }

        @Override
        public void end() {
            ends++;
        }
    }

    private static final class Settings {
        final int up = Config.light_UpRange, down = Config.light_DownRange, horizontal = Config.light_HRange, safe = Config.light_SaveLevel;
        final boolean layer = Config.light_IgnoreLayer, spawn = Config.light_IgnoreSpawn;

        void restore() {
            Config.light_UpRange = up;
            Config.light_DownRange = down;
            Config.light_HRange = horizontal;
            Config.light_SaveLevel = safe;
            Config.light_IgnoreLayer = layer;
            Config.light_IgnoreSpawn = spawn;
        }
    }
}
