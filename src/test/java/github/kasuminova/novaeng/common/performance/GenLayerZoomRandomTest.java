package github.kasuminova.novaeng.common.performance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GenLayerZoomRandomTest {

    static GenLayer layer(final boolean optimized, final boolean fuzzy, final GenLayer parent) throws Exception {
        final Class<?> type = new WorldgenFixtureLoader(optimized).loadClass("net.minecraft.world.gen.layer."
            + (fuzzy ? "GenLayerFuzzyZoom" : "GenLayerZoom"));
        return (GenLayer) type.getConstructor(long.class, GenLayer.class).newInstance(1000L, parent);
    }

    static GenLayer chain(final boolean optimized) throws Exception {
        final ClassLoader loader = new WorldgenFixtureLoader(optimized);
        final Class<?> zoom = loader.loadClass("net.minecraft.world.gen.layer.GenLayerZoom");
        final Class<?> fuzzy = loader.loadClass("net.minecraft.world.gen.layer.GenLayerFuzzyZoom");
        final PatternLayer pattern = new PatternLayer(2);
        pattern.recording = false;
        GenLayer result = (GenLayer) fuzzy.getConstructor(long.class, GenLayer.class).newInstance(1000L, pattern);
        for (int i = 0; i < 5; i++) {
            result = (GenLayer) zoom.getConstructor(long.class, GenLayer.class).newInstance(1001L + i, result);
        }
        result.initWorldGenSeed(42);
        return result;
    }

    @Test
    void exactLayersKeepValuesParentRequestsRandomStateAndAllocationSizes() throws Exception {
        final Random inputs = new Random(0xB10BE5L);
        final Field chunkSeed = GenLayer.class.getDeclaredField("chunkSeed");
        chunkSeed.setAccessible(true);
        for (final boolean fuzzy : new boolean[]{false, true}) {
            for (int pattern = 0; pattern < 4; pattern++) {
                final PatternLayer oldParent = new PatternLayer(pattern);
                final PatternLayer newParent = new PatternLayer(pattern);
                final GenLayer before = layer(false, fuzzy, oldParent);
                final GenLayer after = layer(true, fuzzy, newParent);
                for (int trial = 0; trial < 100; trial++) {
                    final long seed = inputs.nextLong();
                    before.initWorldGenSeed(seed);
                    after.initWorldGenSeed(seed);
                    final int x = trial < 4 ? new int[]{-30_000_000, 30_000_000, -1, 0}[trial] : inputs.nextInt();
                    final int y = inputs.nextInt();
                    final int width = 1 + inputs.nextInt(40);
                    final int height = 1 + inputs.nextInt(40);
                    IntCache.resetIntCache();
                    final int[] original = before.getInts(x, y, width, height);
                    final int length = original.length;
                    final int[] expected = Arrays.copyOf(original, width * height);
                    final long randomState = chunkSeed.getLong(before);
                    IntCache.resetIntCache();
                    final int[] actual = after.getInts(x, y, width, height);
                    final String context = "fuzzy=" + fuzzy + ", pattern=" + pattern + ", trial=" + trial;
                    assertArrayEquals(expected, Arrays.copyOf(actual, width * height), context);
                    assertEquals(length, actual.length, context);
                    assertEquals(oldParent.request, newParent.request, context);
                    assertEquals(randomState, chunkSeed.getLong(after), context);
                }
            }
        }
    }

    @Test
    void everyModeTiePatternAndFuzzyChoiceMatchesTheActualOriginal() throws Exception {
        for (final boolean fuzzy : new boolean[]{false, true}) {
            final PatternLayer oldParent = new PatternLayer(0);
            final PatternLayer newParent = new PatternLayer(0);
            final GenLayer before = layer(false, fuzzy, oldParent);
            final GenLayer after = layer(true, fuzzy, newParent);
            for (int pattern = 0; pattern < 256; pattern++) {
                oldParent.tile = newParent.tile = new int[]{pattern & 3, (pattern >> 2) & 3, (pattern >> 4) & 3, (pattern >> 6) & 3};
                before.initWorldGenSeed(pattern);
                after.initWorldGenSeed(pattern);
                IntCache.resetIntCache();
                final int[] expected = Arrays.copyOf(before.getInts(0, 0, 2, 2), 4);
                IntCache.resetIntCache();
                assertArrayEquals(expected, Arrays.copyOf(after.getInts(0, 0, 2, 2), 4));
            }
        }
    }

    @Test
    void nestedZoomChainsRetainAllOutputAndRandomStates() throws Exception {
        final GenLayer before = chain(false);
        final GenLayer after = chain(true);
        for (int i = 0; i < 100; i++) {
            before.initWorldGenSeed(i * 65537L);
            after.initWorldGenSeed(i * 65537L);
            final int x = i * 17 - 1000;
            final int y = -i * 13;
            IntCache.resetIntCache();
            final int[] expected = Arrays.copyOf(before.getInts(x, y, 16, 16), 256);
            IntCache.resetIntCache();
            assertArrayEquals(expected, Arrays.copyOf(after.getInts(x, y, 16, 16), 256));
        }
    }

    @Test
    void unknownSubclassesFallBackWithoutReadingTheParent() throws Exception {
        final ClassLoader parent = new WorldgenFixtureLoader(true);
        final String name = "nova/fixtures/CustomZoom";
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, name, null, "net/minecraft/world/gen/layer/GenLayerZoom", null);
        final MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitInsn(Opcodes.LCONST_0);
        constructor.visitInsn(Opcodes.ACONST_NULL);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/world/gen/layer/GenLayerZoom", "<init>",
            "(JLnet/minecraft/world/gen/layer/GenLayer;)V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        writer.visitEnd();
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(parent) {
            Class<?> define() {
                return this.defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.define();
        final Method wrapper = type.getSuperclass().getDeclaredMethod("nova$fixedRandomChoices", int.class,
            int.class, int.class, int.class, Operation.class);
        wrapper.setAccessible(true);
        final int[] expected = {42};
        final Operation<int[]> fallback = arguments -> {
            assertArrayEquals(new Object[]{1, 2, 3, 4}, arguments);
            return expected;
        };
        assertSame(expected, wrapper.invoke(type.getConstructor().newInstance(), 1, 2, 3, 4, fallback));
    }

    static final class PatternLayer extends GenLayer {
        private final int pattern;
        boolean recording = true;
        private int[] tile;
        private String request;

        PatternLayer(final int pattern) {
            super(42);
            this.pattern = pattern;
        }

        @Override
        public int[] getInts(final int x, final int y, final int width, final int height) {
            if (this.recording) {
                this.request = x + ":" + y + ":" + width + ":" + height;
            }
            final int[] values = new int[width * height];
            for (int z = 0; z < height; z++) {
                for (int dx = 0; dx < width; dx++) {
                    final int hash = Integer.rotateLeft((x + dx) * 73428767, 13) ^ (y + z) * 912931;
                    values[dx + z * width] = this.tile != null ? this.tile[(dx & 1) + (z & 1) * 2]
                        : this.pattern == 0 ? 1 : this.pattern == 1 ? hash & 1 : this.pattern == 2 ? hash & 15 : hash;
                }
            }
            return values;
        }
    }
}
