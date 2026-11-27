package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

/** Exact getQuads diagnostic expression only; unexpected bytecode is left untouched. */
public final class EnderIoProfilerLabelTransform {
    private static final String MODEL = "crazypants/enderio/base/render/model/RelayingBakedModel";
    private static final String PROFILER = "crazypants/enderio/util/Profiler";
    private static final String STATE = "net/minecraft/block/state/IBlockState";
    private static final String BUILDER = "java/lang/StringBuilder";
    private static final String METHOD = "(L" + STATE + ";Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;";

    private EnderIoProfilerLabelTransform() {
    }

    public static boolean apply(final ClassNode type) {
        if (!MODEL.equals(type.name)) return false;
        MethodNode target = null;
        for (final MethodNode method : type.methods) {
            if ((method.name.equals("getQuads") || method.name.equals("func_188616_a")) && METHOD.equals(method.desc)) {
                if (target != null) return false;
                target = method;
            }
        }
        if (target == null || !target.tryCatchBlocks.isEmpty()) return false;
        final List<AbstractInsnNode> code = new ObjectArrayList<>();
        for (final AbstractInsnNode node : target.instructions) if (node.getOpcode() >= 0) code.add(node);
        int found = -1;
        int stopCalls = 0;
        for (int i = 0; i < code.size(); i++) {
            if (call(code.get(i), PROFILER, "stop", "(JLjava/lang/String;)V")) stopCalls++;
            if (matches(code, i)) {
                if (found >= 0) return false;
                found = i;
            }
        }
        if (found < 0 || stopCalls != 1) return false;
        final int timer = ((VarInsnNode) code.get(found + 1)).var;
        int starts = 0;
        for (int i = 0; i + 2 < found; i++) {
            if (receiver(code.get(i)) && call(code.get(i + 1), PROFILER, "start", "()J")
                && variable(code.get(i + 2), Opcodes.LSTORE, timer)) starts++;
        }
        if (starts != 1) return false;
        // Metadata between these instructions could be a branch/handler entry. Reject it rather
        // than removing a label or changing a pre-existing control-flow boundary.
        for (int i = found; i < found + 12; i++) {
            if (code.get(i).getNext() != code.get(i + 1)) return false;
        }
        final InsnList replacement = new InsnList();
        replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, PROFILER, "instance", "L" + PROFILER + ";"));
        replacement.add(new VarInsnNode(Opcodes.LLOAD, timer));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "github/kasuminova/novaeng/common/performance/EnderIoProfilerLabels", "stop",
            "(L" + PROFILER + ";JL" + STATE + ";)V", false));
        target.instructions.insertBefore(code.get(found), replacement);
        for (int i = found; i <= found + 12; i++) target.instructions.remove(code.get(i));
        return true;
    }

    private static boolean matches(final List<AbstractInsnNode> c, final int i) {
        if (i + 12 >= c.size()) return false;
        return receiver(c.get(i)) && c.get(i + 1).getOpcode() == Opcodes.LLOAD
            && c.get(i + 2) instanceof TypeInsnNode allocation && allocation.getOpcode() == Opcodes.NEW
            && BUILDER.equals(allocation.desc) && c.get(i + 3).getOpcode() == Opcodes.DUP
            && call(c.get(i + 4), BUILDER, "<init>", "()V")
            && variable(c.get(i + 5), Opcodes.ALOAD, 1)
            && (call(c.get(i + 6), STATE, "getBlock", "()Lnet/minecraft/block/Block;")
                || call(c.get(i + 6), STATE, "func_177230_c", "()Lnet/minecraft/block/Block;"))
            && (call(c.get(i + 7), "net/minecraft/block/Block", "getLocalizedName", "()Ljava/lang/String;")
                || call(c.get(i + 7), "net/minecraft/block/Block", "func_149732_F", "()Ljava/lang/String;"))
            && call(c.get(i + 8), BUILDER, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
            && c.get(i + 9) instanceof LdcInsnNode suffix && " (relayed)".equals(suffix.cst)
            && call(c.get(i + 10), BUILDER, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
            && call(c.get(i + 11), BUILDER, "toString", "()Ljava/lang/String;")
            && call(c.get(i + 12), PROFILER, "stop", "(JLjava/lang/String;)V");
    }

    private static boolean receiver(final AbstractInsnNode node) {
        return node instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
            && PROFILER.equals(field.owner) && "instance".equals(field.name)
            && ("L" + PROFILER + ";").equals(field.desc);
    }

    private static boolean variable(final AbstractInsnNode node, final int opcode, final int index) {
        return node instanceof VarInsnNode variable && variable.getOpcode() == opcode && variable.var == index;
    }

    private static boolean call(final AbstractInsnNode node, final String owner, final String name, final String descriptor) {
        return node instanceof MethodInsnNode method && owner.equals(method.owner)
            && name.equals(method.name) && descriptor.equals(method.desc)
            && method.getOpcode() == ("<init>".equals(name) ? Opcodes.INVOKESPECIAL
                : STATE.equals(owner) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL);
    }
}
