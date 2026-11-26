package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FusionInjectorScanTransformTest {
    private static final String TARGET = "com/brandon3055/draconicevolution/blocks/tileentity/TileFusionCraftingCore";

    @Test
    void patchesActualDependencyAndPreservesVerifiableStackShape() throws Exception {
        final ClassNode type = original();
        assertTrue(FusionInjectorScanTransform.apply(type));
        int scans = 0;
        int views = 0;
        for (final var method : type.methods) if (method.name.equals("updateInjectors")) {
            for (final var instruction : method.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.owner.endsWith("/FusionInjectorPositions")) {
                    if (call.name.equals("scan")) scans++;
                    if (call.name.equals("view")) views++;
                }
                assertFalse(call.owner.equals("com/google/common/collect/Lists") && call.name.equals("newArrayList"));
            }
            new Analyzer<>(new BasicInterpreter()).analyze(type.name, method);
        }
        assertEquals(1, scans);
        assertEquals(1, views);
        assertFalse(FusionInjectorScanTransform.apply(type));
        final ClassNode roundTrip = new ClassNode();
        new ClassReader(bytes(type)).accept(roundTrip, 0);
        assertEquals(type.name, roundTrip.name);
    }

    @Test
    void changedRadiusOrLateAnchorLeavesEntireMethodUntouched() throws IOException {
        final ClassNode radiusType = original();
        for (final var method : radiusType.methods) if (method.name.equals("updateInjectors")) {
            for (final var instruction : method.instructions) if (instruction instanceof IntInsnNode radius
                && radius.getOpcode() == Opcodes.BIPUSH && radius.operand == 16) { radius.operand = 15; break; }
        }
        assertUnchanged(radiusType);
        final ClassNode copyType = original();
        MethodInsnNode lastCopy = null;
        for (final var method : copyType.methods) if (method.name.equals("updateInjectors")) {
            for (final var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                && call.owner.equals("com/google/common/collect/Lists") && call.name.equals("newArrayList")) lastCopy = call;
        }
        assertNotNull(lastCopy);
        lastCopy.name = "differentCopy";
        assertUnchanged(copyType);
    }

    private static void assertUnchanged(final ClassNode type) {
        final byte[] before = bytes(type);
        assertFalse(FusionInjectorScanTransform.apply(type));
        assertArrayEquals(before, bytes(type));
    }

    private static ClassNode original() throws IOException {
        try (final var input = FusionInjectorScanTransformTest.class.getClassLoader().getResourceAsStream(TARGET + ".class")) {
            assertNotNull(input);
            final ClassNode type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            return type;
        }
    }

    private static byte[] bytes(final ClassNode type) {
        final ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }
}
