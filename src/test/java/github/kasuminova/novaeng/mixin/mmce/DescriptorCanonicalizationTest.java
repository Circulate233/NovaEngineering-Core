package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.mmce.DescriptorCanonicalPool;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

/**
 * Standalone differential and concurrency checks; no Minecraft bootstrap or Gradle required.
 */
public final class DescriptorCanonicalizationTest {
    private static final String TARGET = "hellfirepvp/modularmachinery/common/util/IBlockStateDescriptor";
    private static final String MIXIN = "github/kasuminova/novaeng/mixin/mmce/MixinBlockStateDescriptorHash";
    private static int checks;

    static void main(final String[] args) throws Exception {
        final Class<?> baseline = descriptor(Path.of(args[0]), false);
        final Class<?> candidate = descriptor(Path.of(args[0]), true);
        final Constructor<?> before = baseline.getDeclaredConstructor(List.class);
        final Constructor<?> after = candidate.getDeclaredConstructor(List.class);
        before.setAccessible(true);
        after.setAccessible(true);
        final var internBefore = baseline.getMethod("canonicalize");
        final var internAfter = candidate.getMethod("canonicalize");
        final var clear = candidate.getMethod("clearPool");
        final List<Object> representativesBefore = new ArrayList<>();
        final List<Object> representativesAfter = new ArrayList<>();
        final Random random = new Random(18961119);
        for (int i = 0; i < 10000; i++) {
            // Include ordered alternatives, nulls and hash collisions across list implementations.
            final List<Object> values = Arrays.asList(random.nextInt(32), i % 3 == 0 ? null : random.nextInt(4));
            final Object a = internBefore.invoke(before.newInstance(values));
            final Object b = internAfter.invoke(after.newInstance(new ArrayList<>(values)));
            final int index = representativesBefore.indexOf(a);
            if (index < 0) {
                representativesBefore.add(a);
                representativesAfter.add(b);
            } else {
                require(representativesBefore.get(index) == a, "upstream identity");
                require(representativesAfter.get(index) == b, "candidate identity/order");
            }
        }
        final List<Object> values = Arrays.asList(1, null);
        final Object old = internAfter.invoke(after.newInstance(values));
        clear.invoke(null);
        final Object fresh = after.newInstance(values);
        require(internAfter.invoke(fresh) == fresh && fresh != old, "clear must release representatives");
        // Run the actual transplanted mixin overwrite through parallel reflection calls.
        try (final var executor = Executors.newFixedThreadPool(16)) {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 16; worker++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 10000; i++) {
                        if (internAfter.invoke(after.newInstance(values)) != fresh) {
                            throw new AssertionError("parallel representative differs");
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (final var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        poolCollisionsAndResets();
        blockInformationStillLocks(Path.of(args[0]));
        benchmark();
        System.out.println("PASS: " + checks + " differential assertions; 160000 actual descriptor parallel calls; pool resets and lock checks.");
    }

    private static Class<?> descriptor(final Path jar, final boolean optimized) throws Exception {
        final ClassNode original = new ClassNode();
        try (final ZipFile zip = new ZipFile(jar.toFile())) {
            new ClassReader(zip.getInputStream(zip.getEntry(TARGET + ".class"))).accept(original, 0);
        }
        original.methods.removeIf(m -> !(List.of("canonicalize", "clearPool", "hashCode", "equals", "<clinit>").contains(m.name)
            || m.name.equals("<init>") && m.desc.equals("(Ljava/util/List;)V")));
        if (optimized) {
            final ClassNode mixin = read(MIXIN);
            original.methods.removeIf(m -> m.name.equals("canonicalize") || m.name.equals("clearPool"));
            mixin.fields.stream().filter(f -> f.name.equals("nova$descriptors")).forEach(original.fields::add);
            for (final MethodNode method : mixin.methods) {
                if (method.name.equals("canonicalize") || method.name.equals("clearPool")) original.methods.add(method);
                if (method.name.equals("<clinit>")) {
                    final InsnList initialization = method.instructions;
                    initialization.remove(initialization.getLast());
                    original.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow()
                        .instructions.insertBefore(original.methods.stream().filter(m -> m.name.equals("<clinit>"))
                                                                   .findFirst().orElseThrow().instructions.getLast(), initialization);
                }
            }
        }
        // Keep the installed jar's exact list equality/hash methods, avoiding Minecraft-only entry points.
        final String renamed = "nova/test/" + (optimized ? "Candidate" : "Baseline");
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        original.accept(new ClassRemapper(writer, new SimpleRemapper(Map.of(TARGET, renamed, MIXIN, renamed))));
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(DescriptorCanonicalizationTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static void blockInformationStillLocks(final Path jar) throws Exception {
        final ClassNode node = read("github/kasuminova/novaeng/mixin/mmce/MixinBlockInformation");
        final MethodNode method = node.methods.stream().filter(m -> m.name.equals("canonicalize")).findFirst().orElseThrow();
        final MethodNode reset = node.methods.stream().filter(m -> m.name.equals("clearPool")).findFirst().orElseThrow();
        require(Arrays.stream(method.instructions.toArray()).anyMatch(i -> i.getOpcode() == Opcodes.MONITORENTER),
            "parallel BlockInformation pool must retain monitor");
        require(Arrays.stream(method.instructions.toArray()).filter(i -> i.getOpcode() == Opcodes.MONITOREXIT).count() == 2,
            "normal and exceptional exits must release monitor");
        require(Arrays.stream(reset.instructions.toArray()).anyMatch(i -> i.getOpcode() == Opcodes.MONITORENTER),
            "reset must synchronize on the shared pool as well");
        final String name = "hellfirepvp/modularmachinery/common/util/BlockArray$BlockInformation";
        final String mixin = "github/kasuminova/novaeng/mixin/mmce/MixinBlockInformation";
        final String renamed = "nova/test/BlockInformation";
        final ClassNode original = new ClassNode();
        try (final ZipFile zip = new ZipFile(jar.toFile())) {
            new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(original, 0);
        }
        original.fields.removeIf(f -> !List.of("POOL", "matchingStates", "matchingTag", "previewTag", "nbtChecker").contains(f.name));
        original.methods.removeIf(m -> !List.of("hashCode", "equals", "<clinit>").contains(m.name));
        original.methods.add(method);
        original.methods.add(reset);
        final MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/List;)V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitVarInsn(Opcodes.ALOAD, 1);
        constructor.visitFieldInsn(Opcodes.PUTFIELD, name, "matchingStates", "Ljava/util/List;");
        constructor.visitInsn(Opcodes.RETURN);
        original.methods.add(constructor);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        original.accept(new ClassRemapper(writer, new SimpleRemapper(Map.of(name, renamed, mixin, renamed,
            "net/minecraft/nbt/NBTTagCompound", "java/lang/Object",
            "github/kasuminova/mmce/common/helper/AdvancedBlockChecker", "java/lang/Object"))));
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(DescriptorCanonicalizationTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
        final var create = type.getConstructor(List.class);
        final var canonicalize = type.getMethod("canonicalize");
        final var representatives = new ConcurrentHashMap<Integer, Object>();
        try (final var executor = Executors.newFixedThreadPool(16)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < 16; w++) {
                final int worker = w;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 5000; i++) {
                        final int key = (i * 31 + worker * 997) & 2047;
                        final Object result = canonicalize.invoke(create.newInstance(List.of(new Key(key))));
                        final Object previous = representatives.putIfAbsent(key, result);
                        if (previous != null && previous != result)
                            throw new AssertionError("BlockInformation pool lost identity");
                    }
                    return null;
                }));
            }
            for (final var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        require(representatives.size() == 2048, "all BlockInformation values survive concurrent growth");
        type.getMethod("clearPool").invoke(null);
        final Object firstAfterClear = create.newInstance(List.of(new Key(1)));
        require(canonicalize.invoke(firstAfterClear) == firstAfterClear, "reset must forget block representatives");
    }

    private static void poolCollisionsAndResets() throws Exception {
        final DescriptorCanonicalPool<Key> pool = new DescriptorCanonicalPool<>();
        final Key first = new Key(1);
        require(pool.canonicalize(first) == first, "first wins");
        require(pool.canonicalize(new Key(1)) == first, "equal keys intern");
        final Key collision = new Key(33);
        require(pool.canonicalize(collision) == collision, "colliding unequal keys remain distinct");
        try (final var executor = Executors.newFixedThreadPool(16)) {
            final List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < 16; w++) {
                final int worker = w;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 20000; i++) {
                        if (worker < 2 && i % 31 == 0) pool.clear();
                        else if (pool.canonicalize(new Key(i % 1024)).value != i % 1024)
                            throw new AssertionError("collision/reset corruption");
                    }
                }));
            }
            for (final var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        pool.clear();
        final Key fresh = new Key(1);
        require(pool.canonicalize(fresh) == fresh, "fresh generation");
    }

    private static ClassNode read(final String name) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var input = DescriptorCanonicalizationTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static void benchmark() throws Exception {
        for (int trial = 0; trial < 4; trial++) {
            for (final boolean striped : new boolean[]{false, true}) {
                final ObjectOpenHashSet<Integer> single = new ObjectOpenHashSet<>();
                final DescriptorCanonicalPool<Integer> pool = new DescriptorCanonicalPool<>();
                final AtomicInteger count = new AtomicInteger();
                final CountDownLatch start = new CountDownLatch(1);
                try (final var executor = Executors.newFixedThreadPool(16)) {
                    final List<Future<?>> futures = new ArrayList<>();
                    for (int w = 0; w < 16; w++) {
                        final int worker = w;
                        futures.add(executor.submit(() -> {
                            start.await();
                            int sum = 0;
                            for (int i = 0; i < 100000; i++) {
                                final Integer value = (i * 31 + worker * 997) & 8191;
                                if (striped) sum += pool.canonicalize(value);
                                else synchronized (single) {
                                    sum += single.addOrGet(value);
                                }
                            }
                            count.addAndGet(sum);
                            return null;
                        }));
                    }
                    final long begin = System.nanoTime();
                    start.countDown();
                    for (final var future : futures) future.get(30, TimeUnit.SECONDS);
                    System.out.printf("pool benchmark trial=%d striped=%s calls=1600000 ms=%.3f checksum=%d%n",
                        trial, striped, (System.nanoTime() - begin) / 1e6, count.get());
                }
            }
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private record Key(int value) {
        @Override
        public int hashCode() {
            return value & 31;
        }
    }
}
