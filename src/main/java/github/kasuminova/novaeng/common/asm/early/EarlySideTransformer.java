package github.kasuminova.novaeng.common.asm.early;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces only the existing copying predicate, leaving its branch and side stripping intact.
 */
public final class EarlySideTransformer implements IExplicitTransformer {
    public static final String TARGET = "net.minecraftforge.fml.common.asm.transformers.SideTransformer";
    private static final String DESCRIPTOR = "Lnet/minecraftforge/fml/relauncher/SideOnly;";
    private static final String HELPER = "github/kasuminova/novaeng/common/asm/early/SideOnlyByteScan";

    private static boolean replacePredicate(final MethodNode method) {
        final List<AbstractInsnNode> code = new ArrayList<>();
        for (final var instruction : method.instructions) if (instruction.getOpcode() >= 0) code.add(instruction);
        if (code.size() < 14
            || !aload3(code.get(0))
            || !(code.get(1) instanceof JumpInsnNode nonNull) || nonNull.getOpcode() != Opcodes.IFNONNULL
            || code.get(2).getOpcode() != Opcodes.ACONST_NULL || code.get(3).getOpcode() != Opcodes.ARETURN
            || !(code.get(4) instanceof TypeInsnNode allocation) || allocation.getOpcode() != Opcodes.NEW || !allocation.desc.equals("java/lang/String")
            || code.get(5).getOpcode() != Opcodes.DUP || !aload3(code.get(6))
            || !(code.get(7) instanceof FieldInsnNode charset) || charset.getOpcode() != Opcodes.GETSTATIC
            || !charset.owner.equals("java/nio/charset/StandardCharsets") || !charset.name.equals("ISO_8859_1") || !charset.desc.equals("Ljava/nio/charset/Charset;")
            || !call(code.get(8), Opcodes.INVOKESPECIAL, "<init>", "([BLjava/nio/charset/Charset;)V")
            || !(code.get(9) instanceof LdcInsnNode needle) || !DESCRIPTOR.equals(needle.cst)
            || !call(code.get(10), Opcodes.INVOKEVIRTUAL, "contains", "(Ljava/lang/CharSequence;)Z")
            || !(code.get(11) instanceof JumpInsnNode present) || present.getOpcode() != Opcodes.IFNE
            || !aload3(code.get(12)) || code.get(13).getOpcode() != Opcodes.ARETURN
            || nextCode(nonNull.label) != code.get(4)
            || nextCode(present.label) != (code.size() > 14 ? code.get(14) : null)) return false;
        // No interior label: unknown incoming branches/exception boundaries must retain the original.
        for (var instruction = code.get(4).getNext(); instruction != code.get(10); instruction = instruction.getNext()) {
            if (instruction instanceof LabelNode || instruction instanceof FrameNode) return false;
        }
        final InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 3));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "containsSideOnly", "([B)Z", false));
        method.instructions.insertBefore(code.get(4), replacement);
        for (int i = 4; i <= 10; i++) method.instructions.remove(code.get(i));
        return true;
    }

    private static AbstractInsnNode nextCode(AbstractInsnNode node) {
        while (node != null && node.getOpcode() < 0) node = node.getNext();
        return node;
    }

    private static boolean aload3(final AbstractInsnNode instruction) {
        return instruction instanceof VarInsnNode load && load.getOpcode() == Opcodes.ALOAD && load.var == 3;
    }

    private static boolean call(final AbstractInsnNode instruction, final int opcode, final String name, final String descriptor) {
        return instruction instanceof MethodInsnNode call && call.getOpcode() == opcode && !call.itf
            && call.owner.equals("java/lang/String") && call.name.equals(name) && call.desc.equals(descriptor);
    }

    @Override
    public byte[] transform(final byte[] bytes) {
        final ClassReader reader = new ClassReader(bytes);
        final ClassNode node = new ClassNode();
        reader.accept(node, 0);
        if (!node.name.equals(TARGET.replace('.', '/'))) return bytes;
        final List<MethodNode> targets = node.methods.stream()
                                                     .filter(method -> method.name.equals("transform") && method.desc.equals("(Ljava/lang/String;Ljava/lang/String;[B)[B"))
                                                     .toList();
        if (targets.size() != 1 || !replacePredicate(targets.getFirst())) {
            LogManager.getLogger("NOVAENG_CORE_PRE").warn("Early side patch shape changed; keeping original transformer");
            return bytes;
        }
        // The replacement leaves the same boolean on the stack; all original branch targets/frames remain.
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        LogManager.getLogger("NOVAENG_CORE_PRE").info("Applied early SideTransformer byte scan");
        return writer.toByteArray();
    }
}
