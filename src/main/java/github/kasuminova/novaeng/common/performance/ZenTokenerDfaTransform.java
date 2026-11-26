package github.kasuminova.novaeng.common.performance;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Whole-expression replacement, keeping the original path if another transformer changes its shape.
 */
public final class ZenTokenerDfaTransform {
    private static final String TOKENER = "stanhebben/zenscript/ZenTokener";
    private static final String PARSER = "stanhebben/zenscript/parser/";

    private ZenTokenerDfaTransform() {
    }

    public static boolean apply(final ClassNode type) {
        if (!TOKENER.equals(type.name)) return false;
        for (final var method : type.methods) {
            if (!"<clinit>".equals(method.name)) continue;
            AbstractInsnNode start = null;
            for (final var instruction : method.instructions) {
                if (matches(instruction)) {
                    if (start != null) return false;
                    start = instruction;
                }
            }
            if (start == null) return false;
            final InsnList replacement = new InsnList();
            replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, TOKENER, "REGEXPS", "[Ljava/lang/String;"));
            replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, TOKENER, "FINALS", "[I"));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "github/kasuminova/novaeng/common/performance/ZenTokenerDfa", "compile",
                "([Ljava/lang/String;[I)L" + PARSER + "CompiledDFA;", false));
            method.instructions.insertBefore(start, replacement);
            for (int i = 0; i < 8; i++) {
                final var next = start.getNext();
                method.instructions.remove(start);
                start = next;
            }
            return true;
        }
        return false;
    }

    private static boolean matches(AbstractInsnNode instruction) {
        if (!(instruction instanceof TypeInsnNode allocation) || allocation.getOpcode() != Opcodes.NEW
            || !(PARSER + "NFA").equals(allocation.desc)) return false;
        instruction = instruction.getNext();
        if (instruction == null || instruction.getOpcode() != Opcodes.DUP) return false;
        instruction = instruction.getNext();
        if (!field(instruction, "REGEXPS", "[Ljava/lang/String;")) return false;
        instruction = instruction.getNext();
        if (!field(instruction, "FINALS", "[I")) return false;
        instruction = instruction.getNext();
        if (!call(instruction, Opcodes.INVOKESPECIAL, "NFA", "<init>", "([Ljava/lang/String;[I)V")) return false;
        instruction = instruction.getNext();
        if (!call(instruction, Opcodes.INVOKEVIRTUAL, "NFA", "toDFA", "()L" + PARSER + "DFA;")) return false;
        instruction = instruction.getNext();
        if (!call(instruction, Opcodes.INVOKEVIRTUAL, "DFA", "optimize", "()L" + PARSER + "DFA;")) return false;
        instruction = instruction.getNext();
        if (!call(instruction, Opcodes.INVOKEVIRTUAL, "DFA", "compile", "()L" + PARSER + "CompiledDFA;")) return false;
        final var result = instruction.getNext();
        return result instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC
            && TOKENER.equals(field.owner) && "DFA".equals(field.name) && ("L" + PARSER + "CompiledDFA;").equals(field.desc);
    }

    private static boolean field(final AbstractInsnNode instruction, final String name, final String descriptor) {
        return instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
            && TOKENER.equals(field.owner) && name.equals(field.name) && descriptor.equals(field.desc);
    }

    private static boolean call(final AbstractInsnNode instruction, final int opcode, final String owner,
                                final String name, final String descriptor) {
        return instruction instanceof MethodInsnNode call && call.getOpcode() == opcode && !call.itf
            && (PARSER + owner).equals(call.owner) && name.equals(call.name) && descriptor.equals(call.desc);
    }
}
