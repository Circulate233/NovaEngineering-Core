package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executes the real constructor mixin with the final-field flags present in the server runtime. */
class ResourceLocationRuntimeFieldsTest {
    private static final String TARGET = "net/minecraft/util/ResourceLocation";
    private static final String MIXIN = "github/kasuminova/novaeng/mixin/minecraft/MixinResourceLocation";
    private static final Set<String> SHARED_FIELDS = Set.of("namespace", "path");

    private static ClassNode read(final String name) throws Exception {
        try (final var input = ResourceLocationRuntimeFieldsTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            final ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private static byte[] bytes(final ClassNode node) {
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transform(final boolean legacyShadows) throws Exception {
        final ClassNode original = read(TARGET);
        // The development access transformers remove these flags, hiding the runtime failure.
        // Restore only the two actual runtime flags; keep the real constructor and all methods.
        original.version = Math.max(original.version, Opcodes.V9);
        assertEquals(2, original.fields.stream().filter(field -> SHARED_FIELDS.contains(field.name)).count());
        original.fields.stream().filter(field -> SHARED_FIELDS.contains(field.name))
            .forEach(field -> field.access |= Opcodes.ACC_FINAL);
        final ClassNode mixin = read(MIXIN);
        if (legacyShadows) {
            // Reproduce the previous implementation by removing its missing annotations before
            // the real Mixin transformation, rather than re-adding final after a successful fix.
            mixin.fields.stream().filter(field -> SHARED_FIELDS.contains(field.name)).forEach(field -> {
                if (field.visibleAnnotations != null) field.visibleAnnotations.removeIf(annotation ->
                    annotation.desc.equals("Lorg/spongepowered/asm/mixin/Final;")
                        || annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mutable;"));
                if (field.invisibleAnnotations != null) field.invisibleAnnotations.removeIf(annotation ->
                    annotation.desc.equals("Lorg/spongepowered/asm/mixin/Final;")
                        || annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mutable;"));
            });
        }
        return IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.minecraft",
            new String[]{TARGET}, Map.of(TARGET, bytes(original), MIXIN, bytes(mixin)), "MixinResourceLocation")[0];
    }

    private static Class<?> define(final byte[] transformed) {
        final String probe = "github/kasuminova/novaeng/mixin/minecraft/RuntimeResourceLocationProbe";
        final ClassNode renamed = new ClassNode();
        new ClassReader(transformed).accept(new ClassRemapper(renamed, new SimpleRemapper(TARGET, probe)), 0);
        final byte[] code = bytes(renamed);
        return new ClassLoader(ResourceLocationRuntimeFieldsTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, code, 0, code.length);
            }
        }.define();
    }

    private static String value(final Class<?> type, final Object instance, final String method) throws Exception {
        return (String) type.getMethod(method).invoke(instance);
    }

    @Test
    void legacyShadowsActuallyThrowTheReportedIllegalAccessError() throws Exception {
        final Class<?> type = define(transform(true));
        assertTrue(java.lang.reflect.Modifier.isFinal(type.getDeclaredField("namespace").getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(type.getDeclaredField("path").getModifiers()));
        final var error = assertThrows(InvocationTargetException.class,
            () -> type.getConstructor(String.class).newInstance("novaeng:runtime_regression"));
        final IllegalAccessError cause = assertInstanceOf(IllegalAccessError.class, error.getCause());
        assertTrue(cause.getMessage().contains("namespace"));
        assertTrue(cause.getMessage().contains("nova$shareFields"));
    }

    @Test
    void mutableShadowsAllowTheRealConstructorAndRetainStringSharingAndCaches() throws Exception {
        final Class<?> type = define(transform(false));
        for (final String field : SHARED_FIELDS) {
            assertFalse(java.lang.reflect.Modifier.isFinal(type.getDeclaredField(field).getModifiers()));
        }
        final var constructor = type.getConstructor(String.class, String.class);
        final Object first = constructor.newInstance(new String("NOVAENG"), new String("Some/Path"));
        final Object second = constructor.newInstance(new String("NOVAENG"), new String("Some/Path"));
        assertEquals("novaeng", value(type, first, "getNamespace"));
        assertEquals("some/path", value(type, first, "getPath"));
        assertSame(value(type, first, "getNamespace"), value(type, second, "getNamespace"));
        assertSame(value(type, first, "getPath"), value(type, second, "getPath"));
        assertEquals("novaeng:some/path", first.toString());
        assertSame(first.toString(), first.toString());
        assertSame(first.toString(), second.toString());
        assertEquals(31 * "novaeng".hashCode() + "some/path".hashCode(), first.hashCode());
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void publicConstructorsAndParserKeepDefaultNamespaceCaseAndUnicodeBehavior() throws Exception {
        final Class<?> type = define(transform(false));
        final var single = type.getConstructor(String.class);
        assertEquals("novaeng:some/path", single.newInstance("NOVAENG:Some/Path").toString());
        assertEquals("minecraft:path", single.newInstance("Path").toString());
        assertEquals("minecraft:path", single.newInstance("a:Path").toString());
        assertEquals("minecraft:path", type.getConstructor(String.class, String.class)
            .newInstance(null, "Path").toString());
        assertEquals("minecraft:path", type.getConstructor(String.class, String.class)
            .newInstance("", "Path").toString());
        final String namespace = "\u0130\u03a3";
        final String path = "PATH/\u0130\u03a3";
        final Object unicode = type.getConstructor(String.class, String.class).newInstance(namespace, path);
        assertEquals(namespace.toLowerCase(Locale.ROOT), value(type, unicode, "getNamespace"));
        assertEquals(path.toLowerCase(Locale.ROOT), value(type, unicode, "getPath"));
        assertArrayEquals(new String[]{"MiXeD", "Some/Path"},
            (String[]) type.getMethod("splitObjectName", String.class).invoke(null, "MiXeD:Some/Path"));
    }
}
