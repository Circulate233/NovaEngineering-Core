package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateIdMapsMixinTest {

    @TempDir
    Path folder;

    private static ClassNode transformed(final String name) throws Exception {
        final ClassNode node = new ClassNode();
        new ClassReader(PerformanceMixinTestLoader.transformed(name)).accept(node, 0);
        return node;
    }

    @Test
    void actualCompassMixinUsesTheOriginalFieldWithPrimitiveLookupsAndSignedPacking() throws Exception {
        final Class<?> type = new PerformanceMixinTestLoader().loadClass("appeng.services.compass.CompassReader");
        final Object reader = type.getConstructor(int.class, File.class).newInstance(1000, this.folder.toFile());
        final Field field = type.getDeclaredField("regions");
        field.setAccessible(true);
        assertEquals(Map.class, field.getType());
        assertInstanceOf(Long2ObjectOpenHashMap.class, field.get(reader));
        final Method get = type.getDeclaredMethod("getRegion", int.class, int.class);
        get.setAccessible(true);
        final Map<Long, Object> expected = new HashMap<>();
        for (final int x : new int[]{0, -1, 1, 1024, -1025, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            for (final int z : new int[]{0, -1, 1024, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                long key = x >> 10;
                key <<= 32;
                key |= z >> 10;
                final Object region = get.invoke(reader, x, z);
                if (expected.containsKey(key)) {
                    assertSame(expected.get(key), region);
                } else {
                    expected.put(key, region);
                }
                assertSame(region, get.invoke(reader, x, z));
            }
        }
        assertEquals(expected.size(), ((Map<?, ?>) field.get(reader)).size());
        type.getMethod("close").invoke(reader);
        assertTrue(((Map<?, ?>) field.get(reader)).isEmpty());
    }

    @Test
    void actualAtmosphereMixinKeepsItsFieldLayoutAndGeneratesTheConstructorInvoker() throws Exception {
        final String name = "zmaster587.advancedRocketry.atmosphere.AtmosphereHandler";
        final ClassNode node = transformed(name);
        assertEquals("Ljava/util/HashMap;", node.fields.stream().filter(field -> field.name.equals("dimensionOxygen"))
                                                       .findFirst().orElseThrow().desc);
        final String primitive = node.fields.stream().filter(field -> field.desc.equals("Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"))
                                            .findFirst().orElseThrow().name;
        // Only the numerical map and actual merged methods are needed; unrelated AR static game setup is not booted.
        node.methods.removeIf(method -> method.name.equals("<clinit>"));
        final MethodNode init = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        init.instructions.add(new TypeInsnNode(Opcodes.NEW, "it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap"));
        init.instructions.add(new InsnNode(Opcodes.DUP));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap", "<init>", "()V", false));
        init.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, primitive, "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"));
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(init);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(this.getClass().getClassLoader()) {
            Class<?> define() {
                return this.defineClass(name, bytes, 0, bytes.length);
            }
        }.define();
        final Field mapField = type.getDeclaredField(primitive);
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked") final Int2ObjectOpenHashMap<Object> map = (Int2ObjectOpenHashMap<Object>) mapField.get(null);
        final MethodNode factory = node.methods.stream().filter(method -> method.name.contains("createHandler")
            && method.desc.equals("(I)Lzmaster587/advancedRocketry/atmosphere/AtmosphereHandler;")).findFirst().orElseThrow();
        final Method create = type.getDeclaredMethod(factory.name, int.class);
        create.setAccessible(true);
        for (final int id : new int[]{0, -1, 1, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            final Object handler = create.invoke(null, id);
            map.put(id, handler);
            assertEquals(true, type.getMethod("hasAtmosphereHandler", int.class).invoke(null, id));
            assertSame(handler, type.getMethod("getOxygenHandler", int.class).invoke(null, id));
            map.remove(id);
            assertEquals(false, type.getMethod("hasAtmosphereHandler", int.class).invoke(null, id));
            assertNull(type.getMethod("getOxygenHandler", int.class).invoke(null, id));
        }
    }

    @Test
    void realMixinTransformedMethodsVerifyAndDoNotBoxMapKeys() throws Exception {
        for (final String name : new String[]{"appeng.services.compass.CompassReader",
            "zmaster587.advancedRocketry.atmosphere.AtmosphereHandler"}) {
            final ClassNode node = transformed(name);
            for (final MethodNode method : node.methods) {
                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                    new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
                }
                if (method.name.equals("getRegion") || method.name.equals("registerWorld") || method.name.equals("unregisterWorld")
                    || method.name.equals("getOxygenHandler") || method.name.equals("hasAtmosphereHandler")) {
                    for (final var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call) {
                            assertFalse(call.name.equals("valueOf") && (call.owner.equals("java/lang/Integer") || call.owner.equals("java/lang/Long")),
                                method.name + ": boxed map key");
                        }
                    }
                }
            }
        }
    }

    public static class IntFixture {
        private static final Map<Integer, Object> VALUES = new HashMap<>();

        public static Object put(final int key, final Object value) {
            return VALUES.put(key, value);
        }

        public static Object get(final int key) {
            return VALUES.get(key);
        }
    }

    public static class PrimitiveIntFixture {
        private static final Int2ObjectOpenHashMap<Object> VALUES = new Int2ObjectOpenHashMap<>(4);

        public static Object put(final int key, final Object value) {
            return VALUES.put(key, value);
        }

        public static Object get(final int key) {
            return VALUES.get(key);
        }
    }

    public static class LongFixture {
        private final Map<Long, Object> values = new HashMap<>(100);

        public Object put(final long key, final Object value) {
            return this.values.put(key, value);
        }

        public Object get(final long key) {
            return this.values.get(key);
        }
    }

    public static class PrimitiveLongFixture {
        private final Long2ObjectOpenHashMap<Object> values = new Long2ObjectOpenHashMap<>(4);

        public Object put(final long key, final Object value) {
            return this.values.put(key, value);
        }

        public Object get(final long key) {
            return this.values.get(key);
        }
    }
}
