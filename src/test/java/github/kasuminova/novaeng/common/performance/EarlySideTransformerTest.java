package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.asm.early.EarlySideTransformer;
import github.kasuminova.novaeng.common.asm.early.SideOnlyByteScan;
import net.minecraft.launchwrapper.IClassTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlySideTransformerTest {
    private static final String NEEDLE = "Lnet/minecraftforge/fml/relauncher/SideOnly;";

    private static void checkScan(final byte[] bytes) {
        assertEquals(new String(bytes, StandardCharsets.ISO_8859_1).contains(NEEDLE), SideOnlyByteScan.containsSideOnly(bytes));
    }

    private static byte[] sample(final String classSide, final String memberSide) {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/Sample", null, "java/lang/Object", null);
        if (classSide != null) {
            final var annotation = writer.visitAnnotation(NEEDLE, true);
            annotation.visitEnum("value", "Lnet/minecraftforge/fml/relauncher/Side;", classSide);
            annotation.visitEnd();
        }
        final var field = writer.visitField(Opcodes.ACC_PUBLIC, "field", "I", null, null);
        if (memberSide != null) {
            final var annotation = field.visitAnnotation(NEEDLE, true);
            annotation.visitEnum("value", "Lnet/minecraftforge/fml/relauncher/Side;", memberSide);
            annotation.visitEnd();
        }
        field.visitEnd();
        final var method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "method", "()V", null, null);
        if (memberSide != null) {
            final var annotation = method.visitAnnotation(NEEDLE, true);
            annotation.visitEnum("value", "Lnet/minecraftforge/fml/relauncher/Side;", memberSide);
            annotation.visitEnd();
        }
        method.visitCode();
        method.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;",
            new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false),
            Type.getMethodType("()V"), new Handle(Opcodes.H_INVOKESTATIC, "test/Sample", "lambda$method$0", "()V", false), Type.getMethodType("()V"));
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        final var lambda = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "lambda$method$0", "()V", null, null);
        lambda.visitCode();
        lambda.visitInsn(Opcodes.RETURN);
        lambda.visitMaxs(0, 0);
        lambda.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetBytes() throws IOException {
        try (final var stream = EarlySideTransformerTest.class.getClassLoader().getResourceAsStream(EarlySideTransformer.TARGET.replace('.', '/') + ".class")) {
            assertNotNull(stream);
            return stream.readAllBytes();
        }
    }

    @Test
    void scanExactlyMatchesLatin1ContainsIncludingMalformedInputs() {
        final byte[] needle = NEEDLE.getBytes(StandardCharsets.ISO_8859_1);
        final Random random = new Random(7319701);
        for (int length = 0; length < 512; length++) {
            final byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            checkScan(bytes);
            if (length >= needle.length) {
                for (final int start : new int[]{0, (length - needle.length) / 2, length - needle.length}) {
                    final byte[] withNeedle = bytes.clone();
                    System.arraycopy(needle, 0, withNeedle, start, needle.length);
                    checkScan(withNeedle);
                }
            }
        }
        for (int offset = 0; offset < needle.length; offset++) {
            for (int value = 0; value < 256; value++) {
                final byte[] changed = needle.clone();
                changed[offset] = (byte) value;
                checkScan(changed);
            }
        }
        final byte[] repeated = new byte[1024];
        Arrays.fill(repeated, (byte) 'L');
        checkScan(repeated);
        assertThrows(NullPointerException.class, () -> SideOnlyByteScan.containsSideOnly(null));
    }

    @Test
    void productionTransformerPreservesStrippingErrorsAndLambdaRemovalOnBothSides() throws Exception {
        for (final String side : new String[]{"CLIENT", "SERVER"}) {
            final IClassTransformer original = new Isolated(false, side).target();
            final IClassTransformer patched = new Isolated(true, side).target();
            assertNull(patched.transform("test.Sample", "test.Sample", null));
            for (final String classSide : new String[]{null, "CLIENT", "SERVER"}) {
                for (final String memberSide : new String[]{null, "CLIENT", "SERVER"}) {
                    final byte[] bytes = sample(classSide, memberSide);
                    if (classSide != null && !classSide.equals(side)) {
                        final RuntimeException expected = assertThrows(RuntimeException.class, () -> original.transform("a", "test.Sample", bytes));
                        final RuntimeException actual = assertThrows(RuntimeException.class, () -> patched.transform("a", "test.Sample", bytes));
                        assertEquals(expected.getClass(), actual.getClass());
                        assertEquals(expected.getMessage(), actual.getMessage());
                    } else {
                        final byte[] expected = original.transform("a", "test.Sample", bytes);
                        final byte[] actual = patched.transform("a", "test.Sample", bytes);
                        assertArrayEquals(expected, actual);
                        if (classSide == null && memberSide == null) assertSame(bytes, actual);
                        if (memberSide != null && !memberSide.equals(side)) {
                            final ClassNode result = new ClassNode();
                            new ClassReader(actual).accept(result, 0);
                            assertTrue(result.fields.isEmpty());
                            assertTrue(result.methods.isEmpty(), "Side-only method and its synthetic lambda must both be removed");
                        }
                    }
                }
            }
            final byte[] malformedNegative = new byte[]{(byte) 255, 0, 3};
            assertSame(malformedNegative, patched.transform("a", "a", malformedNegative));
        }
    }

    @Test
    void unexpectedPredicateIsReturnedUnmodified() throws IOException {
        final byte[] original = targetBytes();
        assertNotSame(original, new EarlySideTransformer().transform(original));
        final ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        node.methods.stream().filter(method -> method.name.equals("transform")).forEach(method -> {
            for (final var instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode literal && NEEDLE.equals(literal.cst))
                    literal.cst = "another predicate";
            }
        });
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] changed = writer.toByteArray();
        assertSame(changed, new EarlySideTransformer().transform(changed));
    }

    private static final class Isolated extends ClassLoader {
        private final boolean patch;
        private final String side;

        Isolated(final boolean patch, final String side) {
            super(EarlySideTransformerTest.class.getClassLoader());
            this.patch = patch;
            this.side = side;
        }

        IClassTransformer target() throws Exception {
            return (IClassTransformer) loadClass(EarlySideTransformer.TARGET).getConstructor().newInstance();
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!name.equals(EarlySideTransformer.TARGET) && !name.startsWith(EarlySideTransformer.TARGET + "$"))
                return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (final var stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        assertNotNull(stream);
                        byte[] bytes = stream.readAllBytes();
                        if (name.equals(EarlySideTransformer.TARGET)) {
                            if (patch) {
                                final byte[] patched = new EarlySideTransformer().transform(bytes);
                                assertNotSame(bytes, patched, "Production predicate must be recognized");
                                bytes = patched;
                            }
                            // Select each side without mutating the test JVM's global FMLLaunchHandler.
                            final ClassNode node = new ClassNode();
                            new ClassReader(bytes).accept(node, 0);
                            final var init = node.methods.stream().filter(method -> method.name.equals("<clinit>")).findFirst().orElseThrow();
                            init.instructions.clear();
                            init.instructions.add(new LdcInsnNode(side));
                            init.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, "SIDE", "Ljava/lang/String;"));
                            init.instructions.add(new InsnNode(Opcodes.RETURN));
                            init.tryCatchBlocks.clear();
                            if (init.localVariables != null) init.localVariables.clear();
                            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                            node.accept(writer);
                            bytes = writer.toByteArray();
                        }
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException error) {
                        throw new ClassNotFoundException(name, error);
                    }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }
    }
}
