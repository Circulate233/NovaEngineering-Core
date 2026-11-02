package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerDimensionIdsMixinTest {

    private static final String SERVER = "net.minecraft.server.MinecraftServer";
    private static final String IDS = "github/kasuminova/novaeng/common/performance/DimensionTickIds";

    private static List<String> calls(final MethodNode method) {
        final List<String> result = new ArrayList<>();
        for (final var instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) {
                continue;
            }
            if (call.owner.equals("java/lang/Integer") && call.name.equals("intValue")) {
                continue;
            }
            result.add(call.owner.equals(IDS) && call.name.equals("get")
                || call.owner.equals("net/minecraftforge/common/DimensionManager") && call.name.equals("getIDs")
                ? "dimensionIds" : call.owner + "." + call.name + call.desc);
        }
        return result;
    }

    private static ClassNode read(final byte[] bytes) {
        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode tick(final ClassNode node) {
        return node.methods.stream().filter(method -> method.name.equals("updateTimeLightAndEntities")).findFirst().orElseThrow();
    }

    @Test
    void actualMixinAppliesAndPreservesTheLifecycleCallOrder() throws Exception {
        final ClassNode before = read(PerformanceMixinTestLoader.resource(SERVER));
        final ClassNode after = read(PerformanceMixinTestLoader.transformed(SERVER));
        final MethodNode original = tick(before);
        final MethodNode optimized = tick(after);
        assertEquals(calls(original), calls(optimized));
        new Analyzer<>(new BasicVerifier()).analyze(after.name, optimized);
        assertTrue(Stream.of(optimized.instructions.toArray()).anyMatch(instruction -> instruction.getOpcode() == Opcodes.IALOAD));
        for (final var instruction : optimized.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertFalse(call.owner.equals("net/minecraftforge/common/DimensionManager") && call.name.equals("getIDs"));
                assertFalse(call.owner.equals("java/lang/Integer") && call.name.equals("intValue"));
            }
        }
    }

    @Test
    void actualOverwriteRetainsTheTwoHundredTickLeakCheckExpression() throws Exception {
        final MethodNode tick = tick(read(PerformanceMixinTestLoader.transformed(SERVER)));
        final MethodInsnNode fetch = (MethodInsnNode) Stream.of(tick.instructions.toArray())
                                                            .filter(instruction -> instruction instanceof MethodInsnNode call && call.owner.equals(IDS) && call.name.equals("get"))
                                                            .findFirst().orElseThrow();
        AbstractInsnNode start = fetch.getPrevious();
        while (!(start instanceof FieldInsnNode field && field.name.equals("tickCounter"))) {
            start = start.getPrevious();
        }
        start = start.getPrevious();
        while (start.getOpcode() < 0) {
            start = start.getPrevious();
        }
        final ClassNode fixture = read(PerformanceMixinTestLoader.resource(Fixture.class.getName()));
        final MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "check", "()V", null, null);
        final Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction = start; instruction != fetch.getNext(); instruction = instruction.getNext()) {
            if (instruction instanceof LabelNode label) {
                labels.put(label, new LabelNode());
            }
        }
        for (AbstractInsnNode instruction = start; instruction != fetch.getNext(); instruction = instruction.getNext()) {
            if (instruction.getType() == AbstractInsnNode.FRAME || instruction.getType() == AbstractInsnNode.LINE) {
                continue;
            }
            final AbstractInsnNode copy = instruction.clone(labels);
            if (copy instanceof FieldInsnNode copiedField) {
                copiedField.owner = fixture.name;
            } else if (copy instanceof MethodInsnNode call) {
                call.owner = fixture.name;
                call.name = "primitiveIds";
            }
            method.instructions.add(copy);
        }
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        fixture.methods.add(method);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        fixture.accept(writer);
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(this.getClass().getClassLoader()) {
            Class<?> define() {
                return this.defineClass(Fixture.class.getName(), bytes, 0, bytes.length);
            }
        }.define();
        final Object instance = type.getConstructor().newInstance();
        for (final int counter : new int[]{0, 1, 199, 200, 201, 400, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            type.getField("tickCounter").setInt(instance, counter);
            type.getMethod("check").invoke(instance);
            assertEquals(counter % 200 == 0, type.getField("lastCheck").getBoolean(null));
        }
    }

    public static class Fixture {
        public static boolean lastCheck;
        public int tickCounter;

        public static int[] primitiveIds(final boolean check) {
            lastCheck = check;
            return new int[0];
        }
    }
}
