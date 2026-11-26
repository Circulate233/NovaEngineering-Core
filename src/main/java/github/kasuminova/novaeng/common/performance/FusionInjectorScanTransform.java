package github.kasuminova.novaeng.common.performance;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/** Removes only DE 2.3.28.193's temporary coordinate collections; injector decisions are unchanged. */
public final class FusionInjectorScanTransform {
    private static final String TARGET = "com/brandon3055/draconicevolution/blocks/tileentity/TileFusionCraftingCore";
    private static final String POS = "net/minecraft/util/math/BlockPos";
    private static final String HELPER = "github/kasuminova/novaeng/common/performance/FusionInjectorPositions";
    private static final String BOX = "(L" + POS + ";L" + POS + ";)Ljava/lang/Iterable;";
    private static final String COPY = "(Ljava/lang/Iterable;)Ljava/util/ArrayList;";

    private FusionInjectorScanTransform() {
    }

    public static boolean apply(final ClassNode type) {
        if (!TARGET.equals(type.name)) return false;
        for (final var method : type.methods) {
            if (!"updateInjectors".equals(method.name) || !"()V".equals(method.desc)) continue;
            final List<AbstractInsnNode> instructions = new ArrayList<>();
            for (final var instruction : method.instructions) if (instruction.getOpcode() >= 0) instructions.add(instruction);
            int start = -1;
            FieldInsnNode position = null;
            for (int i = 0; i + 6 < instructions.size(); i++) {
                if (!allocation(instructions.get(i))) continue;
                if (start >= 0) return false;
                start = i;
                if (!(instructions.get(i + 6) instanceof FieldInsnNode field)
                    || field.getOpcode() != Opcodes.GETFIELD || !("L" + POS + ";").equals(field.desc)
                    || !TARGET.equals(field.owner) || !("pos".equals(field.name) || "field_174879_c".equals(field.name))) return false;
                position = field;
            }
            if (start < 2 || position == null) return false;
            // Radius and local slots are part of the checked expression, not guessed from a partial match.
            if (!(instructions.get(start - 2) instanceof IntInsnNode radius) || radius.getOpcode() != Opcodes.BIPUSH
                || radius.operand != 16 || !variable(instructions.get(start - 1), Opcodes.ISTORE, 1)) return false;
            final List<AbstractInsnNode> expected = expression(position);
            if (start + expected.size() + 1 >= instructions.size()) return false;
            for (int i = 0; i < expected.size(); i++) if (!same(instructions.get(start + i), expected.get(i))) return false;
            final int end = start + expected.size();
            for (var node = instructions.get(start); node != instructions.get(end); node = node.getNext()) {
                // A new basic block means another transformer may branch into this expression.
                if (node instanceof FrameNode) return false;
            }
            if (!variable(instructions.get(end), Opcodes.ALOAD, 2)
                || !call(instructions.get(end + 1), Opcodes.INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;")) return false;
            // The remaining single materialization is the line-of-sight foreach. Check it before any mutation.
            MethodInsnNode lineCopy = null;
            for (int i = end; i < instructions.size(); i++) {
                if (!call(instructions.get(i), Opcodes.INVOKESTATIC, "com/google/common/collect/Lists", "newArrayList", COPY)) continue;
                if (lineCopy != null || i == 0 || !(instructions.get(i - 1) instanceof MethodInsnNode box)
                    || !POS.equals(box.owner) || !BOX.equals(box.desc)
                    || !("getAllInBox".equals(box.name) || "func_177980_a".equals(box.name))) return false;
                lineCopy = (MethodInsnNode) instructions.get(i);
            }
            if (lineCopy == null) return false;

            final InsnList replacement = new InsnList();
            replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
            replacement.add(new FieldInsnNode(Opcodes.GETFIELD, position.owner, position.name, position.desc));
            replacement.add(new VarInsnNode(Opcodes.ILOAD, 1));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "scan", "(L" + POS + ";I)Ljava/util/List;", false));
            replacement.add(new VarInsnNode(Opcodes.ASTORE, 2));
            method.instructions.insertBefore(instructions.get(start), replacement);
            for (int i = start; i < end; i++) method.instructions.remove(instructions.get(i));
            lineCopy.owner = HELPER;
            lineCopy.name = "view";
            lineCopy.desc = "(Ljava/lang/Iterable;)Ljava/util/List;";
            return true;
        }
        return false;
    }

    private static List<AbstractInsnNode> expression(final FieldInsnNode pos) {
        final List<AbstractInsnNode> result = new ArrayList<>();
        result.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        result.add(new InsnNode(Opcodes.DUP));
        result.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false));
        result.add(new VarInsnNode(Opcodes.ASTORE, 2));
        for (int axis = 0; axis < 3; axis++) {
            result.add(new VarInsnNode(Opcodes.ALOAD, 2));
            for (int side = 0; side < 2; side++) {
                result.add(new VarInsnNode(Opcodes.ALOAD, 0));
                result.add(new FieldInsnNode(Opcodes.GETFIELD, pos.owner, pos.name, pos.desc));
                for (int coordinate = 0; coordinate < 3; coordinate++) {
                    if (coordinate == axis) {
                        result.add(new VarInsnNode(Opcodes.ILOAD, 1));
                        if (side == 0) result.add(new InsnNode(Opcodes.INEG));
                    } else result.add(new InsnNode(side == 0 ? Opcodes.ICONST_M1 : Opcodes.ICONST_1));
                }
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, POS, "add", "(III)L" + POS + ";", false));
            }
            result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, POS, "getAllInBox", BOX, false));
            result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/google/common/collect/Lists", "newArrayList", COPY, false));
            result.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "addAll", "(Ljava/util/Collection;)Z", true));
            result.add(new InsnNode(Opcodes.POP));
        }
        return result;
    }

    private static boolean allocation(final AbstractInsnNode node) {
        return node instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && "java/util/ArrayList".equals(type.desc);
    }

    private static boolean variable(final AbstractInsnNode node, final int opcode, final int slot) {
        return node instanceof VarInsnNode variable && variable.getOpcode() == opcode && variable.var == slot;
    }

    private static boolean call(final AbstractInsnNode node, final int opcode, final String owner, final String name, final String desc) {
        return node instanceof MethodInsnNode method && method.getOpcode() == opcode && owner.equals(method.owner)
            && name.equals(method.name) && desc.equals(method.desc);
    }

    private static boolean same(final AbstractInsnNode actual, final AbstractInsnNode expected) {
        if (actual.getOpcode() != expected.getOpcode()) return false;
        if (expected instanceof VarInsnNode variable) return actual instanceof VarInsnNode a && a.var == variable.var;
        if (expected instanceof TypeInsnNode type) return actual instanceof TypeInsnNode a && a.desc.equals(type.desc);
        if (expected instanceof FieldInsnNode field) return actual instanceof FieldInsnNode a && a.owner.equals(field.owner)
            && a.name.equals(field.name) && a.desc.equals(field.desc);
        if (expected instanceof MethodInsnNode method) {
            if (!(actual instanceof MethodInsnNode a) || !a.owner.equals(method.owner) || !a.desc.equals(method.desc) || a.itf != method.itf) return false;
            return a.name.equals(method.name) || POS.equals(method.owner)
                && ("add".equals(method.name) && "func_177982_a".equals(a.name)
                || "getAllInBox".equals(method.name) && "func_177980_a".equals(a.name));
        }
        return true;
    }
}
