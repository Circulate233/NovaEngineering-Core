package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapHeightValues;
import github.kasuminova.novaeng.common.performance.JourneyMapBlockPositions;
import github.kasuminova.novaeng.common.performance.JourneyMapMetadataEpoch;
import github.kasuminova.novaeng.common.performance.JourneyMapSectionWriter;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import journeymap.common.chunk.JMChunkSnapshot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class JourneyMapRuntimeAllocationTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void actualJourneyMapSectionFormatIsByteIdenticalBeforeAndAfterTransform() throws Exception {
        final String target = JMChunkSnapshot.class.getName();
        final byte[] transformed = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.journeymap", new String[]{target}, "MixinJMChunkSnapshotWriter")[0];
        final Class<?> optimizedClass = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass(target, transformed, 0, transformed.length);
            }
        }.define();
        final Method original = sectionMethod(JMChunkSnapshot.class);
        final Method optimized = sectionMethod(optimizedClass);
        for (final ExtendedBlockStorage section : sections()) {
            final byte[] expected = serialize(original, section);
            assertArrayEquals(expected, serialize(optimized, section));
            // Hits, subsequent section reuse and explicit epoch changes must all preserve the format.
            assertArrayEquals(expected, serialize(optimized, section));
            JourneyMapMetadataEpoch.invalidate();
            assertArrayEquals(expected, serialize(optimized, section));
            JourneyMapSectionWriter.clearMetadata();
            assertArrayEquals(expected, serialize(optimized, section));
        }
    }

    @Test
    void sectionScratchSurvivesNestedWritesAndOutputExceptions() throws Exception {
        final ExtendedBlockStorage[] sections = sections();
        final Method original = sectionMethod(JMChunkSnapshot.class);
        final byte[] expectedOuter = serialize(original, sections[1]);
        final byte[] expectedInner = serialize(original, sections[2]);
        final ByteArrayOutputStream outer = new ByteArrayOutputStream();
        final ByteArrayOutputStream inner = new ByteArrayOutputStream();
        final OutputStream reentrant = new OutputStream() {
            boolean entered;

            @Override
            public void write(final int value) throws IOException {
                if (!entered) {
                    entered = true;
                    JourneyMapSectionWriter.write(new DataOutputStream(inner), sections[2]);
                }
                outer.write(value);
            }
        };
        JourneyMapSectionWriter.write(new DataOutputStream(reentrant), sections[1]);
        assertArrayEquals(expectedOuter, outer.toByteArray());
        assertArrayEquals(expectedInner, inner.toByteArray());

        assertThrows(IOException.class, () -> JourneyMapSectionWriter.write(new DataOutputStream(new OutputStream() {
            @Override
            public void write(final int value) throws IOException {
                throw new IOException("intentional output failure");
            }
        }), sections[2]));
        final ByteArrayOutputStream afterFailure = new ByteArrayOutputStream();
        JourneyMapSectionWriter.write(new DataOutputStream(afterFailure), sections[0]);
        assertArrayEquals(serialize(original, sections[0]), afterFailure.toByteArray());
    }

    @Test
    void heightInjectionAppliesToActualSurfaceAndTopoClasses() throws Exception {
        IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.journeymap",
            new String[]{"journeymap.client.cartography.render.SurfaceRenderer",
                "journeymap.client.cartography.render.TopoRenderer"},
            "MixinSurfaceHeightValues", "MixinTopoHeightValues");
    }

    @Test
    void scanCoordinateInjectionAppliesToActualBlockMD() throws Exception {
        IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.journeymap",
            new String[]{"journeymap.client.model.block.BlockMD"}, "MixinBlockMDScanPositions");
    }

    @Test
    void cachedCoordinatesRemainImmutableAcrossEscapesReentryAndExtendedHeightCollisions() {
        final BlockPos escaped = JourneyMapBlockPositions.atWorld(-17, -64, 33);
        assertEquals(BlockPos.class, escaped.getClass());
        assertSame(escaped, JourneyMapBlockPositions.atWorld(-17, -64, 33));
        // Simulate a nested lookup and a different chunk with the same cache index.
        final BlockPos nested = JourneyMapBlockPositions.atWorld(-1, 192, 49);
        assertNotSame(escaped, nested);
        assertEquals(new BlockPos(-17, -64, 33), escaped);
        assertEquals(new BlockPos(-1, 192, 49), nested);
        assertEquals(escaped, JourneyMapBlockPositions.atWorld(-17, -64, 33));
        for (final int y : new int[]{Integer.MIN_VALUE, -4096, -1, 0, 255, 256, 4095, Integer.MAX_VALUE}) {
            assertEquals(new BlockPos(-30000000, y, 30000000), JourneyMapBlockPositions.atWorld(-30000000, y, 30000000));
        }
        // Returning to the outer query must not mutate a position retained by an inner backend either.
        assertEquals(new BlockPos(-1, 192, 49), nested);
    }

    @Test
    void registryInvalidationAppliesToActualForgeRegistryWithoutLoadingJourneyMap() throws Exception {
        IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.journeymap",
            new String[]{"net.minecraftforge.registries.ForgeRegistry"}, "MixinForgeRegistryMetadataEpoch");
    }

    @Test
    void boxedHeightCachePreservesExtendedBoundsAndRacingPublication() {
        for (final int value : new int[]{Integer.MIN_VALUE, -4097, -4096, -129, -128, 0, 127, 128, 255,
            1024, 4095, 4096, Integer.MAX_VALUE}) {
            assertEquals(value, JourneyMapHeightValues.valueOf(value).intValue());
        }
        final var futures = new ObjectArrayList<CompletableFuture<Integer>>();
        for (int i = 0; i < 16; i++) futures.add(CompletableFuture.supplyAsync(() -> JourneyMapHeightValues.valueOf(2467)));
        final Integer winner = futures.getFirst().join();
        for (int index = 0; index < futures.size(); index++) assertSame(winner, futures.get(index).join());
    }

    private static Method sectionMethod(final Class<?> owner) throws Exception {
        final Method method = owner.getDeclaredMethod("writeSectionBlockStates", DataOutput.class, ExtendedBlockStorage.class);
        method.setAccessible(true);
        return method;
    }

    private static byte[] serialize(final Method method, final ExtendedBlockStorage section) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        method.invoke(null, new DataOutputStream(bytes), section);
        return bytes.toByteArray();
    }

    private static ExtendedBlockStorage[] sections() {
        final ExtendedBlockStorage air = new ExtendedBlockStorage(0, true);
        final ExtendedBlockStorage mixed = new ExtendedBlockStorage(16, false);
        final ExtendedBlockStorage widePalette = new ExtendedBlockStorage(-16, true);
        final ObjectArrayList<IBlockState> states = new ObjectArrayList<>();
        for (final Block block : Block.REGISTRY) states.addAll(block.getBlockState().getValidStates());
        assertTrue(states.size() > 256, "Exercise the high byte of section palette indices using actual registered states");
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    final int index = y * 256 + z * 16 + x;
                    mixed.set(x, y, z, index % 3 == 0 ? Blocks.WATER.getDefaultState() : Blocks.STONE.getDefaultState());
                    widePalette.set(x, y, z, states.get(index % Math.min(states.size(), 1024)));
                }
            }
        }
        return new ExtendedBlockStorage[]{air, mixed, widePalette};
    }
}
