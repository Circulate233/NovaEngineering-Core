package github.kasuminova.novaeng.client.memory;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.PUTFIELD;

/**
 * Forces readers into the exact field-publication window without timing-dependent large allocations.
 */
public class AnimationFramePublicationTest {
    private static final String OWNER = OwnedAnimationFrames.class.getName();
    private static final String CALLBACK = AnimationFramePublicationTest.class.getName().replace('.', '/');
    private static volatile Gate active;

    private static Object packed(final Class<?> type) throws Exception {
        final Object frames = type.getConstructor().newInstance();
        @SuppressWarnings("unchecked") final List<int[][]> internal = (List<int[][]>) type.getMethod("internalView").invoke(frames);
        for (int frame = 0; frame < 6; frame++) {
            final int[][] levels = new int[5][];
            for (int level = 0; level < levels.length; level++) levels[level] = pixels(frame, level);
            internal.add(levels);
        }
        type.getMethod("pack").invoke(frames);
        return frames;
    }

    private static int[] pixels(final int frame, final int level) {
        final int[] pixels = new int[(64 >> level) * (64 >> level)];
        Arrays.fill(pixels, 0xff123400 | frame * 11 + level + 1);
        return pixels;
    }

    private static void assertFrame(final int[][] actual, final int frame) {
        assertNotNull(actual);
        assertEquals(5, actual.length);
        for (int level = 0; level < actual.length; level++) {
            assertArrayEquals(pixels(frame, level), actual[level], "incomplete published mip " + level);
        }
    }

    private static int[][] copy(final int[][] levels) {
        if (levels == null) return null;
        final int[][] copy = levels.clone();
        for (int level = 0; level < copy.length; level++) if (copy[level] != null) copy[level] = copy[level].clone();
        return copy;
    }

    private static Object field(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(final Method method, final Object target, final Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }

    private static <T> T observePublication(final String field, final Callable<T> read) throws Exception {
        return observePublication(field, read, read);
    }

    private static <T> T observePublication(final String field, final Callable<T> firstRead, final Callable<T> secondRead) throws Exception {
        final ExecutorService threads = Executors.newFixedThreadPool(2);
        final Gate gate = new Gate(field);
        active = gate;
        try (threads) {
            try {
                final Future<T> first = threads.submit(() -> {
                    gate.writer = Thread.currentThread();
                    return firstRead.call();
                });
                assertTrue(gate.entered.await(3, TimeUnit.SECONDS), "writer did not reach " + field + " publication");
                final Future<T> second = threads.submit(secondRead);
                T observed = null;
                boolean serialized = false;
                try {
                    observed = second.get(150, TimeUnit.MILLISECONDS);
                } catch (final TimeoutException guardedByLock) {
                    // A lock is also valid: permit the writer to finish before waiting for the second reader.
                    serialized = true;
                } finally {
                    gate.resume.await(3, TimeUnit.SECONDS);
                }
                first.get(3, TimeUnit.SECONDS);
                return serialized ? second.get(3, TimeUnit.SECONDS) : observed;
            } finally {
                active = null;
                threads.shutdownNow();
            }
        }
    }

    // Invoked by the bytecode inserted by PausingLoader below.
    @SuppressWarnings("unused")
    public static void afterPublication(final String field) throws Exception {
        final Gate gate = active;
        if (gate == null || !gate.field.equals(field) || gate.writer != Thread.currentThread() || gate.entered.getCount() == 0)
            return;
        gate.entered.countDown();
        gate.resume.await(3, TimeUnit.SECONDS);
    }

    @Test
    void publicFrameNeverPublishesAnOuterArrayBeforeItsMipLevelsAreReady() throws Exception {
        for (int round = 0; round < 8; round++) {
            final Object frames = packed(new PausingLoader().loadClass(OWNER));
            final Method get = frames.getClass().getMethod("get", int.class);
            final int[][] observed = observePublication("source", () -> copy((int[][]) invoke(get, frames, 0)));
            assertFrame(observed, 0);
            assertFrame((int[][]) invoke(get, frames, 0), 0);
        }
    }

    @Test
    void decodedMipNeverPublishesItsRawArrayBeforeThePixelsAreReady() throws Exception {
        for (int round = 0; round < 8; round++) {
            final Object frames = packed(new PausingLoader().loadClass(OWNER));
            final Object ownership = field(frames, "ownership");
            final Object frame = ((List<?>) field(frames, "frames")).getFirst();
            final Object mip = Array.get(field(frame, "mips"), 0);
            assertNull(field(mip, "raw"), "test requires a compressed mip");
            final Method stable = mip.getClass().getDeclaredMethod("stable", ownership.getClass());
            stable.setAccessible(true);
            final int[] observed = observePublication("raw", () -> ((int[]) invoke(stable, mip, ownership)).clone());
            assertArrayEquals(pixels(0, 0), observed, "a concurrent decoder observed unfinished raw pixels");
        }
    }

    @Test
    void privateDecodeCanFinishWhenPublicReadingReleasesTheCompressedPayload() throws Exception {
        for (int round = 0; round < 8; round++) {
            final Object frames = packed(new PausingLoader().loadClass(OWNER));
            final Method get = frames.getClass().getMethod("get", int.class);
            final Object lock = frames.getClass().getMethod("readLock").invoke(frames);
            @SuppressWarnings("unchecked") final List<int[][]> internal = (List<int[][]>) frames.getClass().getMethod("internalView").invoke(frames);
            final int[][] observed = observePublication("payload-read", () -> {
                synchronized (lock) {
                    final int[][] decoded = copy(internal.getFirst());
                    assertFrame(decoded, 0);
                    return decoded;
                }
            }, () -> copy((int[][]) invoke(get, frames, 0)));
            assertFrame(observed, 0);
        }
    }

    @Test
    void concurrentPublicAndVanillaPrivateReadersKeepEveryMipComplete() throws Exception {
        final ExecutorService readers = Executors.newFixedThreadPool(2);
        try (readers) {
            try {
                for (int round = 0; round < 64; round++) {
                    final OwnedAnimationFrames frames = (OwnedAnimationFrames) packed(OwnedAnimationFrames.class);
                    final List<int[][]> privateFrames = frames.internalView();
                    final CyclicBarrier start = new CyclicBarrier(2);
                    final Future<int[][]> published = readers.submit(() -> {
                        start.await(3, TimeUnit.SECONDS);
                        return copy(frames.getFirst());
                    });
                    final Future<int[][]> internal = readers.submit(() -> {
                        start.await(3, TimeUnit.SECONDS);
                        synchronized (frames.readLock()) {
                            return copy(privateFrames.getFirst());
                        }
                    });
                    assertFrame(published.get(3, TimeUnit.SECONDS), 0);
                    assertFrame(internal.get(3, TimeUnit.SECONDS), 0);
                }
            } finally {
                readers.shutdownNow();
            }
        }
    }

    @Test
    void privateConsumersKeepBothBorrowedFramesUntilTheirConsumptionFinishes() throws Exception {
        final ExecutorService readers = Executors.newFixedThreadPool(2);
        try (readers) {
            try {
                for (int round = 0; round < 32; round++) {
                    final OwnedAnimationFrames frames = (OwnedAnimationFrames) packed(OwnedAnimationFrames.class);
                    final List<int[][]> internal = frames.internalView();
                    final Object lock = frames.readLock();
                    final CountDownLatch borrowed = new CountDownLatch(1);
                    final CountDownLatch secondAttempt = new CountDownLatch(1);
                    final CountDownLatch finishConsumption = new CountDownLatch(1);
                    final Future<?> first = readers.submit(() -> {
                        synchronized (lock) {
                            final int[][] current = internal.get(0);
                            final int[][] next = internal.get(1);
                            borrowed.countDown();
                            assertTrue(finishConsumption.await(3, TimeUnit.SECONDS));
                            assertFrame(current, 0);
                            assertFrame(next, 1);
                        }
                        return null;
                    });
                    assertTrue(borrowed.await(3, TimeUnit.SECONDS));
                    final Future<?> second = readers.submit(() -> {
                        secondAttempt.countDown();
                        synchronized (lock) {
                            for (final int frame : new int[]{5, 2, 4, 3, 0})
                                assertFrame(copy(internal.get(frame)), frame);
                        }
                        return null;
                    });
                    try {
                        assertTrue(secondAttempt.await(3, TimeUnit.SECONDS));
                        assertFalse(second.isDone(), "another consumer must wait until both borrowed frames are consumed");
                    } finally {
                        finishConsumption.countDown();
                    }
                    first.get(3, TimeUnit.SECONDS);
                    second.get(3, TimeUnit.SECONDS);
                }
            } finally {
                readers.shutdownNow();
            }
        }
    }

    private static final class Gate {
        final String field;
        final CountDownLatch entered = new CountDownLatch(1);
        final CyclicBarrier resume = new CyclicBarrier(2);
        volatile Thread writer;

        Gate(final String field) {
            this.field = field;
        }
    }

    private static final class PausingLoader extends ClassLoader {
        PausingLoader() {
            super(AnimationFramePublicationTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!name.equals(OWNER) && !name.startsWith(OWNER + '$')) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try (final var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        final ClassNode node = new ClassNode();
                        new ClassReader(input).accept(node, ClassReader.SKIP_FRAMES);
                        for (final MethodNode method : node.methods) {
                            if (!method.name.equals("stable") && !(name.endsWith("$Window") && method.name.equals("read")))
                                continue;
                            for (final AbstractInsnNode instruction : method.instructions.toArray()) {
                                if (instruction instanceof FieldInsnNode field && field.getOpcode() == PUTFIELD &&
                                    (name.endsWith("$Frame") && field.name.equals("source") || name.endsWith("$Mip") && field.name.equals("raw"))) {
                                    final InsnList pause = new InsnList();
                                    pause.add(new LdcInsnNode(field.name));
                                    pause.add(new MethodInsnNode(INVOKESTATIC, CALLBACK, "afterPublication", "(Ljava/lang/String;)V", false));
                                    method.instructions.insert(instruction, pause);
                                } else if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETFIELD &&
                                    name.endsWith("$Window") && field.name.equals("payload")) {
                                    final InsnList pause = new InsnList();
                                    pause.add(new LdcInsnNode("payload-read"));
                                    pause.add(new MethodInsnNode(INVOKESTATIC, CALLBACK, "afterPublication", "(Ljava/lang/String;)V", false));
                                    method.instructions.insertBefore(instruction, pause);
                                }
                            }
                        }
                        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                        node.accept(writer);
                        final byte[] bytes = writer.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (final IOException failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        }
    }
}
