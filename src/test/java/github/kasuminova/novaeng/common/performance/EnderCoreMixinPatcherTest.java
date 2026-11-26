package github.kasuminova.novaeng.common.performance;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import github.kasuminova.novaeng.common.asm.early.EnderCoreMixinPatcherTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnderCoreMixinPatcherTest {
    private static byte[] sample(final String... interfaces) {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/Example", null, "java/lang/Object", interfaces);
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Test
    void retainsDirectTargetsAndInterfacesButSkipsUnrelatedClasses() throws Exception {
        final String target = EnderCoreMixinPatcherTransformer.TARGET;
        final String internalFixture = Type.getInternalName(Fixture.class);
        final ClassWriter renamed = new ClassWriter(0);
        try (final var fixture = Fixture.class.getResourceAsStream("/" + internalFixture + ".class")) {
            new ClassReader(fixture)
                .accept(new ClassRemapper(renamed, new SimpleRemapper(internalFixture, target.replace('.', '/'))), 0);
        }
        final byte[] original = renamed.toByteArray();
        final byte[] patched = new EnderCoreMixinPatcherTransformer().transform(original);
        assertNotSame(original, patched);

        final Multimap<String, String> interfaces = HashMultimap.create();
        interfaces.put("test/Target", "patch");
        final Class<?> type = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass(target, patched, 0, patched.length);
            }
        }.define();
        final Object patcher = type.getConstructor(Multimap.class).newInstance(interfaces);
        final var transform = type.getMethod("transform", String.class, String.class, byte[].class);
        final byte[] withoutMatch = sample("test/Other");
        final byte[] withMatch = sample("test/Other", "test/Target");
        assertSame(withoutMatch, transform.invoke(patcher, "a", "a", withoutMatch));
        assertArrayEquals(new byte[]{7}, (byte[]) transform.invoke(patcher, "a", "direct", withoutMatch));
        assertArrayEquals(new byte[]{7}, (byte[]) transform.invoke(patcher, "a", "a", withMatch));
        interfaces.clear();
        assertSame(withMatch, transform.invoke(patcher, "a", "a", withMatch));
    }

    public static class Fixture {
        private final Multimap<String, String> interfaceTargets;

        public Fixture(final Multimap<String, String> interfaceTargets) {
            this.interfaceTargets = interfaceTargets;
        }

        public byte[] transform(final String name, final String transformedName, final byte[] bytes) {
            final List<String> targets = "direct".equals(transformedName) ? List.of(transformedName) : List.of();
            if (targets.isEmpty() && interfaceTargets.isEmpty()) return bytes;
            new ClassNode();
            return new byte[]{7};
        }
    }
}
