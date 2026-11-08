package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.asm.early.EarlyEventTransformer;
import github.kasuminova.novaeng.common.asm.early.EventClassScan;
import net.minecraft.launchwrapper.IClassTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EarlyEventTransformerTest {
    private static final String ANNOTATION = "Lnet/minecraftforge/fml/common/eventhandler/SubscribeEvent;";

    @Test
    void subscriberPatchKeepsOriginalBytesErrorsAndAccessChanges() throws Exception {
        final IClassTransformer original = (IClassTransformer) new Isolated(EarlyEventTransformer.SUBSCRIBER, false).target();
        final IClassTransformer patched = (IClassTransformer) new Isolated(EarlyEventTransformer.SUBSCRIBER, true).target();
        assertNull(patched.transform("test.Empty", "test.Empty", null));
        for (final int access : new int[]{Opcodes.ACC_PUBLIC, Opcodes.ACC_PROTECTED, 0, Opcodes.ACC_PRIVATE}) {
            for (final boolean annotate : new boolean[]{false, true}) {
                final byte[] bytes = sample(access, annotate, "java/lang/Object");
                if (annotate && access == Opcodes.ACC_PRIVATE) {
                    final RuntimeException expected = assertThrows(RuntimeException.class, () -> original.transform("test.Sample", "test.Sample", bytes));
                    final RuntimeException actual = assertThrows(RuntimeException.class, () -> patched.transform("test.Sample", "test.Sample", bytes));
                    assertEquals(expected.getMessage(), actual.getMessage());
                } else {
                    assertArrayEquals(original.transform("test.Sample", "test.Sample", bytes), patched.transform("test.Sample", "test.Sample", bytes));
                    if (!annotate) assertSame(bytes, patched.transform("test.Sample", "test.Sample", bytes));
                }
            }
        }
        assertTrue(EventClassScan.maySubscribe(new byte[]{1, 2, 3}));
        assertFalse(EventClassScan.maySubscribe(sample(0, false, "java/lang/Object")));
    }

    @Test
    void subscriptionPatchAvoidsObjectLookupAndRetainsEventGeneration() throws Exception {
        final Isolated originalLoader = new Isolated(EarlyEventTransformer.SUBSCRIPTION, false);
        final Isolated patchedLoader = new Isolated(EarlyEventTransformer.SUBSCRIPTION, true);
        final IClassTransformer original = (IClassTransformer) originalLoader.target();
        final IClassTransformer patched = (IClassTransformer) patchedLoader.target();
        final byte[] ordinary = sample(0, false, "java/lang/Object");
        // Resolve the helper before counting lookups made by the actual transform invocation.
        patched.transform("test.Sample", "test.Sample", ordinary);
        originalLoader.objectLookups = 0;
        patchedLoader.objectLookups = 0;
        assertSame(ordinary, original.transform("test.Sample", "test.Sample", ordinary));
        assertSame(ordinary, patched.transform("test.Sample", "test.Sample", ordinary));
        assertTrue(originalLoader.objectLookups > 0);
        assertEquals(0, patchedLoader.objectLookups);
        final byte[] event = sample(Opcodes.ACC_PUBLIC, false, "net/minecraftforge/fml/common/eventhandler/Event");
        assertArrayEquals(original.transform("test.Sample", "test.Sample", event), patched.transform("test.Sample", "test.Sample", event));
        final byte[] missing = sample(0, false, "test/UnavailableParent");
        assertSame(missing, patched.transform("test.Sample", "test.Sample", missing));
    }

    private static byte[] sample(final int access, final boolean annotate, final String parent) {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, 0, "test/Sample", null, parent, null);
        writer.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "longConstant", "J", null, 9L).visitEnd();
        writer.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "doubleConstant", "D", null, 1.5).visitEnd();
        final var method = writer.visitMethod(access, "listen", "()V", null, null);
        if (annotate) method.visitAnnotation(ANNOTATION, true).visitEnd();
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static final class Isolated extends ClassLoader {
        private final String name;
        private final boolean patch;
        private int objectLookups;
        Isolated(final String name, final boolean patch) { super(EarlyEventTransformerTest.class.getClassLoader()); this.name = name; this.patch = patch; }
        Object target() throws Exception { return loadClass(name).getConstructor().newInstance(); }
        @Override protected Class<?> loadClass(final String className, final boolean resolve) throws ClassNotFoundException {
            if (className.equals("java.lang.Object")) objectLookups++;
            if (!className.equals(name) && !className.startsWith(name + "$")) return super.loadClass(className, resolve);
            synchronized (getClassLoadingLock(className)) {
                Class<?> loaded = findLoadedClass(className);
                if (loaded == null) {
                    try (final var input = getParent().getResourceAsStream(className.replace('.', '/') + ".class")) {
                        assertNotNull(input, className);
                        byte[] bytes = input.readAllBytes();
                        if (patch && className.equals(name)) bytes = new EarlyEventTransformer().transform(bytes);
                        loaded = defineClass(className, bytes, 0, bytes.length);
                    } catch (IOException error) { throw new ClassNotFoundException(className, error); }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }
    }
}
