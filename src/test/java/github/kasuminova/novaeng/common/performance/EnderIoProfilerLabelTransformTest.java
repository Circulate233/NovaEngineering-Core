package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderIoProfilerLabelTransformTest {
    private static final String RESOURCE = "crazypants/enderio/base/render/model/RelayingBakedModel.class";

    private static ClassNode original() throws Exception {
        final ClassNode node = new ClassNode();
        final String production = System.getProperty("novaeng.test.productionEnderIoJar");
        if (production != null) {
            try (JarFile jar = new JarFile(Path.of(production).toFile());
                 InputStream input = jar.getInputStream(jar.getJarEntry(RESOURCE))) {
                new ClassReader(input).accept(node, 0);
            }
        } else {
            try (InputStream input = EnderIoProfilerLabelTransformTest.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                assertNotNull(input, "EnderIO runtime dependency is required for the real bytecode fixture");
                new ClassReader(input).accept(node, 0);
            }
        }
        return node;
    }

    private static byte[] bytes(final ClassNode node) {
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void names(final ClassNode node, final boolean named) {
        for (final var method : node.methods) {
            if (method.name.equals("func_188616_a") || method.name.equals("getQuads"))
                method.name = named ? "getQuads" : "func_188616_a";
            for (final var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals("net/minecraft/block/state/IBlockState")
                    && (call.name.equals("getBlock") || call.name.equals("func_177230_c")))
                    call.name = named ? "getBlock" : "func_177230_c";
                if (call.owner.equals("net/minecraft/block/Block")
                    && (call.name.equals("getLocalizedName") || call.name.equals("func_149732_F")))
                    call.name = named ? "getLocalizedName" : "func_149732_F";
            }
        }
    }

    @Test
    void acceptsRealEnderIoBodyWithProductionAndDevelopmentNames() throws Exception {
        for (final boolean named : new boolean[]{false, true}) {
            final ClassNode node = original();
            names(node, named);
            assertTrue(EnderIoProfilerLabelTransform.apply(node));
            int helpers = 0;
            for (final var method : node.methods) {
                for (final var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call
                        && call.owner.equals("github/kasuminova/novaeng/common/performance/EnderIoProfilerLabels")) helpers++;
                }
            }
            assertEquals(1, helpers);
            final byte[] transformed = bytes(node);
            assertFalse(EnderIoProfilerLabelTransform.apply(node));
            assertArrayEquals(transformed, bytes(node));
        }
    }

    @Test
    void rejectsChangedSuffixOrUnexpectedCallWithoutPartialMutation() throws Exception {
        final ClassNode suffixChanged = original();
        for (final var method : suffixChanged.methods) {
            for (final var instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode ldc && " (relayed)".equals(ldc.cst)) ldc.cst = " (changed)";
            }
        }
        final byte[] before = bytes(suffixChanged);
        assertFalse(EnderIoProfilerLabelTransform.apply(suffixChanged));
        assertArrayEquals(before, bytes(suffixChanged));

        final ClassNode getterChanged = original();
        for (final var method : getterChanged.methods) {
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/block/Block")
                    && (call.name.equals("getLocalizedName") || call.name.equals("func_149732_F"))) call.name = "differentName";
            }
        }
        final byte[] getterBefore = bytes(getterChanged);
        assertFalse(EnderIoProfilerLabelTransform.apply(getterChanged));
        assertArrayEquals(getterBefore, bytes(getterChanged));
    }
}
