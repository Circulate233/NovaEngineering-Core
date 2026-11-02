package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorCompassMetadataTest {

    static final PropertyInteger META = PropertyInteger.create("metadata", 0, 15);
    private static final String ORIGINAL = "com.circulation.better_meteorite.mixin.ae2.MixinCompassService";

    static Scan scan(final boolean optimized, final int match, final boolean recording) throws Exception {
        final ClassNode node = optimized ? optimizedScanner() : original();
        final byte[] bytes = bytes(node);
        final Class<?> type = new ClassLoader(MeteorCompassMetadataTest.class.getClassLoader()) {
            Class<?> define() {
                return this.defineClass(ORIGINAL, bytes, 0, bytes.length);
            }
        }.define();
        final Object instance = type.getConstructor().newInstance();
        final MetadataBlock background = block("background");
        final MetadataBlock target = block("target");
        final ScanChunk chunk = WorldgenTestBlocks.allocate(ScanChunk.class);
        chunk.states = new IBlockState[8192];
        Arrays.fill(chunk.states, background.getDefaultState());
        if (match >= 0) {
            chunk.states[match] = target.getDefaultState().withProperty(META, 3);
        }
        chunk.recording = recording;
        chunk.visits = new ObjectArrayList<>();
        final ScanWorld world = WorldgenTestBlocks.allocate(ScanWorld.class);
        world.chunk = chunk;
        final CaptureExecutor executor = new CaptureExecutor();
        set(type, null, "CHUNK_SIZE", 16);
        set(type, instance, "executor", executor);
        final Constructor<?> post = Post.class.getConstructor(Object.class, World.class, int.class, int.class, int.class, boolean.class);
        post.setAccessible(true);
        set(type, null, "randomComplement$constructor", post);
        final Scan result = new Scan(type, instance, type.getMethod("updateAreaMixin", World.class, int.class, int.class,
            int.class, CallbackInfoReturnable.class), world, executor, background, target);
        result.targets(List.of(target.getRegistryName()), List.of(3));
        return result;
    }

    private static MetadataBlock block(final String name) throws Exception {
        final MetadataBlock block = WorldgenTestBlocks.states(WorldgenTestBlocks.allocate(MetadataBlock.class), META);
        return registered(block, new ResourceLocation("nova", name));
    }

    static <T extends Block> T registered(final T block, final ResourceLocation name) throws Exception {
        final Class<?> delegateType = Class.forName("net.minecraftforge.registries.RegistryDelegate");
        final Constructor<?> constructor = delegateType.getDeclaredConstructor(Object.class, Class.class);
        constructor.setAccessible(true);
        final Object delegate = constructor.newInstance(block, Block.class);
        final Method setName = delegateType.getDeclaredMethod("setName", ResourceLocation.class);
        setName.setAccessible(true);
        setName.invoke(delegate, name);
        final Field field = IForgeRegistryEntry.Impl.class.getField("delegate");
        field.setAccessible(true);
        field.set(block, delegate);
        return block;
    }

    private static ClassNode original() throws Exception {
        try (final var stream = MeteorCompassMetadataTest.class.getClassLoader()
                                                               .getResourceAsStream(ORIGINAL.replace('.', '/') + ".class")) {
            assertNotNull(stream, "Missing actual BetterMeteorite dependency");
            final ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static ClassNode optimizedScanner() throws Exception {
        final ClassNode fixture = new ClassNode();
        new ClassReader(PerformanceMixinTestLoader.transformed("appeng.services.CompassService"))
            .accept(new ClassRemapper(fixture, new SimpleRemapper("appeng/services/CompassService", ORIGINAL.replace('.', '/'))), 0);
        fixture.methods.removeIf(method -> method.name.equals("<init>") || method.name.equals("<clinit>"));
        for (final MethodNode method : original().methods) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                fixture.methods.add(method);
            }
        }
        for (final var field : fixture.fields) {
            field.access &= ~Opcodes.ACC_FINAL;
        }
        final MethodNode handler = fixture.methods.stream().filter(method -> method.name.contains("updateAreaMixin")
                                              && method.desc.equals("(Lnet/minecraft/world/World;IIILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"))
                                                  .findFirst().orElseThrow();
        handler.access = (handler.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
        handler.name = "updateAreaMixin";
        return fixture;
    }

    private static byte[] bytes(final ClassNode node) {
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void set(final Class<?> type, final Object instance, final String name, final Object value) throws Exception {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    @Test
    void actualScannerRetainsRegistryIdentityMetadataVisitsAndSubmissions() throws Exception {
        for (final int match : new int[]{-1, 0, 31, 1024, 8191}) {
            final Scan before = scan(false, match, true);
            final Scan after = scan(true, match, true);
            for (final int y : new int[]{-1, 0, 31, 32, 127, 255, 256}) {
                before.invoke(-33, y, 1000);
                after.invoke(-33, y, 1000);
                assertEquals(before.world.chunk.visits, after.world.chunk.visits);
                assertEquals(before.world.chunk.checksum, after.world.chunk.checksum);
                assertEquals(before.executor.last.found, after.executor.last.found);
                assertEquals(match >= 0, after.executor.last.found);
                assertEquals(-3, after.executor.last.chunkX);
                assertEquals(62, after.executor.last.chunkZ);
                assertEquals(y >> 5, after.executor.last.band);
                assertSame(after.world, after.executor.last.world);
                assertEquals(before.executor.submissions, after.executor.submissions);
            }
            assertEquals(0, after.background.metadataCalls);
            if (match != 0) {
                assertTrue(before.background.metadataCalls > 0);
            }
        }
    }

    @Test
    void equalButDistinctRegistryNamesAreNotMatchedAndNoStateResultIsCached() throws Exception {
        final Scan before = scan(false, 8191, true);
        final Scan after = scan(true, 8191, true);
        final ResourceLocation equalName = new ResourceLocation(after.target.getRegistryName().toString());
        assertNotSame(after.target.getRegistryName(), equalName);
        assertEquals(after.target.getRegistryName(), equalName);
        after.targets(List.of(equalName), List.of(3));
        before.targets(List.of(new ResourceLocation(before.target.getRegistryName().toString())), List.of(3));
        before.invoke(0, 32, 0);
        after.invoke(0, 32, 0);
        assertFalse(before.executor.last.found);
        assertFalse(after.executor.last.found);
        after.targets(List.of(after.target.getRegistryName(), after.target.getRegistryName()), List.of(2, 3));
        final int previousCalls = after.target.metadataCalls;
        after.invoke(0, 32, 0);
        assertTrue(after.executor.last.found);
        assertEquals(previousCalls + 1, after.target.metadataCalls);
        after.world.chunk.states[8191] = after.background.getDefaultState();
        after.invoke(0, 32, 0);
        assertFalse(after.executor.last.found);
        after.world.chunk.states[0] = after.target.getDefaultState().withProperty(META, 2);
        after.invoke(0, 32, 0);
        assertTrue(after.executor.last.found);
    }

    @Test
    void uninitializedTargetsStillUseTheOriginalCallbackPath() throws Exception {
        for (final boolean optimized : new boolean[]{false, true}) {
            final Scan scan = scan(optimized, -1, true);
            set(scan.type, null, "randomComplement$MeteoriteCompassTarget", null);
            final var failure = assertThrows(InvocationTargetException.class, () -> scan.invoke(0, 32, 0));
            assertInstanceOf(NullPointerException.class, failure.getCause());
            assertTrue(scan.world.chunk.visits.isEmpty());
        }
    }

    @Test
    void ordinaryMixinWrapsTheRealMergedScannerWithoutAPluginPatch() throws Exception {
        final ClassNode target = new ClassNode();
        new ClassReader(PerformanceMixinTestLoader.transformed("appeng.services.CompassService")).accept(target, 0);
        assertTrue(target.methods.stream().anyMatch(method -> method.name.contains("nova$scanArea")));
        for (final MethodNode method : target.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                new Analyzer<>(new BasicVerifier()).analyze(target.name, method);
            }
        }
    }

    record Scan(Class<?> type, Object instance, Method method, ScanWorld world, CaptureExecutor executor,
                MetadataBlock background, MetadataBlock target) {
        @SuppressWarnings("unchecked")
        void targets(final List<ResourceLocation> names, final List<Integer> values) throws Exception {
            set(this.type, null, "randomComplement$MeteoriteCompassTarget", new ObjectArrayList<>(names));
            final Field field = this.type.getDeclaredField("randomComplement$meta");
            field.setAccessible(true);
            final List<Integer> metadata = (List<Integer>) field.get(null);
            metadata.clear();
            metadata.addAll(values);
        }

        void invoke(final int x, final int y, final int z) throws Exception {
            final CallbackInfoReturnable<Future<?>> callback = new CallbackInfoReturnable<>("test", true);
            this.method.invoke(this.instance, this.world, x, y, z, callback);
            assertNotNull(callback.getReturnValue());
            assertTrue(callback.isCancelled());
        }
    }

    static final class MetadataBlock extends Block {
        int metadataCalls;

        private MetadataBlock() {
            super(Material.ROCK);
        }

        @Override
        public int getMetaFromState(final IBlockState state) {
            this.metadataCalls++;
            return state.getValue(META);
        }
    }

    static final class ScanChunk extends Chunk {
        IBlockState[] states;
        List<BlockPos> visits;
        boolean recording;
        long checksum;

        private ScanChunk() {
            super(null, 0, 0);
        }

        @Override
        public IBlockState getBlockState(final int x, final int y, final int z) {
            if (this.recording) {
                this.visits.add(new BlockPos(x, y, z));
            }
            this.checksum = this.checksum * 31 + x * 8192L + z * 512L + y;
            return this.states[(x * 16 + z) * 32 + (y & 31)];
        }
    }

    static final class ScanWorld extends World {
        ScanChunk chunk;

        private ScanWorld() {
            super(null, null, null, null, false);
        }

        @Override
        public Chunk getChunk(final int x, final int z) {
            return this.chunk;
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

    public static final class Post implements Runnable {
        final World world;
        final int chunkX;
        final int chunkZ;
        final int band;
        final boolean found;

        public Post(final Object service, final World world, final int chunkX, final int chunkZ, final int band, final boolean found) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.band = band;
            this.found = found;
        }

        @Override
        public void run() {
        }
    }

    static final class CaptureExecutor extends AbstractExecutorService {
        Post last;
        int submissions;

        @Override
        public Future<?> submit(final Runnable task) {
            this.last = (Post) task;
            this.submissions++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(final Runnable command) {
            throw new AssertionError();
        }
    }
}
