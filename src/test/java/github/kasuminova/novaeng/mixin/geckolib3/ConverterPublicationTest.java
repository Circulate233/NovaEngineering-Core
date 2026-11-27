package github.kasuminova.novaeng.mixin.geckolib3;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises the current mapper mixin with the installed GeckoLib's initialization code.
 */
public final class ConverterPublicationTest {
    private static final String TARGET = "software.bernie.geckolib3.geo.raw.pojo.Converter";
    private static final AtomicInteger attempts = new AtomicInteger();
    private static volatile boolean failFirst;

    static void main(final String[] args) throws Exception {
        check(false);
        check(true);
        System.out.println("PASS: 16 concurrent reader/writer callers; one mapper initialization; failed first initialization retried; JSON round trip.");
    }

    public static void beforeMapper() {
        if (attempts.incrementAndGet() == 1 && failFirst) throw new IllegalStateException("injected first-use failure");
    }

    private static void check(final boolean fail) throws Exception {
        attempts.set(0);
        failFirst = fail;
        final ClassLoader loader = new ClassLoader(ConverterPublicationTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (!name.equals(TARGET) && !name.startsWith(TARGET + "$")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        try {
                            final ClassNode original = read(name.replace('.', '/'));
                            if (name.equals(TARGET)) {
                                final String mixinName = "github/kasuminova/novaeng/mixin/geckolib3/MixinGeckoConverter";
                                final ClassNode mixin = new ClassNode();
                                read(mixinName).accept(new ClassRemapper(mixin,
                                    new SimpleRemapper(mixinName, TARGET.replace('.', '/'))));
                                mixin.fields.stream().filter(f -> f.name.equals("nova$mapperReady")).forEach(original.fields::add);
                                original.methods.removeIf(m -> m.name.equals("getObjectReader") || m.name.equals("getObjectWriter"));
                                mixin.methods.stream().filter(m -> List.of("getObjectReader", "getObjectWriter", "nova$ensureMapper")
                                                                       .contains(m.name)).forEach(original.methods::add);
                                original.methods.stream().filter(m -> m.name.equals("instantiateMapper")).findFirst().orElseThrow()
                                    .instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                            ConverterPublicationTest.class.getName().replace('.', '/'), "beforeMapper", "()V", false));
                            }
                            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                            original.accept(writer);
                            final byte[] bytes = writer.toByteArray();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (final Exception failure) {
                            throw new ClassNotFoundException(name, failure);
                        }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        };
        final Class<?> converter = loader.loadClass(TARGET);
        final var reader = converter.getDeclaredMethod("getObjectReader");
        final var writer = converter.getDeclaredMethod("getObjectWriter");
        reader.setAccessible(true);
        writer.setAccessible(true);
        if (fail) {
            try {
                reader.invoke(null);
                throw new AssertionError("failure injection was ignored");
            } catch (final InvocationTargetException expected) {
                if (!(expected.getCause() instanceof IllegalStateException)) throw expected;
            }
        }
        final List<Object[]> results = new ObjectArrayList<>();
        try (final var executor = Executors.newFixedThreadPool(16)) {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<Object[]>> futures = new ObjectArrayList<>();
            for (int i = 0; i < 16; i++) {
                final boolean writerFirst = i % 2 == 0;
                futures.add(executor.submit(() -> {
                    start.await();
                    if (writerFirst) writer.invoke(null);
                    return new Object[]{reader.invoke(null), writer.invoke(null)};
                }));
            }
            start.countDown();
            for (int i = 0; i < futures.size(); i++) results.add(futures.get(i).get(30, TimeUnit.SECONDS));
        }
        if (attempts.get() != (fail ? 2 : 1)) throw new AssertionError("duplicate mapper initialization: " + attempts);
        for (int i = 0; i < results.size(); i++) {
            final Object[] result = results.get(i);
            if (result[0] == null || result[1] == null || result[0] != results.getFirst()[0] || result[1] != results.getFirst()[1])
                throw new AssertionError("accessors were not published together");
        }
        final Object model = converter.getMethod("fromJsonString", String.class).invoke(null,
            "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[]}");
        final String json = (String) converter.getMethod("toJsonString", model.getClass()).invoke(null, model);
        if (!json.contains("1.12.0") || !json.contains("minecraft:geometry")) throw new AssertionError(json);
    }

    private static ClassNode read(final String name) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var input = ConverterPublicationTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }
}
