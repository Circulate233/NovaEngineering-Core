package github.kasuminova.novaeng.common.performance;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** A guarded, whole-expression patch; all anchors are checked before any mutation. */
public final class RuntimeEventAllocationTransform {
    private static final String EVENT = "xyz/phanta/tconevo/client/event/ItemStackBarEvent";
    private static final String SET = "java/util/LinkedHashSet";

    private RuntimeEventAllocationTransform() { }

    public static boolean apply(final ClassNode type) {
        if (!EVENT.equals(type.name) || type.fields.stream().noneMatch(field ->
            "bars".equals(field.name) && "Ljava/util/Set;".equals(field.desc))) return false;
        MethodNode constructor = null;
        for (final MethodNode method : type.methods) {
            if ("<init>".equals(method.name)
                && "(Lnet/minecraft/item/ItemStack;)V".equals(method.desc)) {
                if (constructor != null) return false;
                constructor = method;
            }
        }
        if (constructor == null) return false;
        TypeInsnNode found = null;
        for (final var instruction : constructor.instructions) {
            if (!(instruction instanceof TypeInsnNode allocation)
                || instruction.getOpcode() != Opcodes.NEW || !SET.equals(allocation.desc)) continue;
            if (found != null || !matches(allocation)) return false;
            found = allocation;
        }
        if (found == null) return false;
        final var duplicate = found.getNext();
        final var initializer = duplicate.getNext();
        constructor.instructions.set(found, new MethodInsnNode(Opcodes.INVOKESTATIC,
            "github/kasuminova/novaeng/client/performance/LazyLinkedSet", "create", "()Ljava/util/Set;", false));
        constructor.instructions.remove(duplicate);
        constructor.instructions.remove(initializer);
        return true;
    }

    private static boolean matches(final TypeInsnNode allocation) {
        final var duplicate = allocation.getNext();
        if (duplicate == null || duplicate.getOpcode() != Opcodes.DUP) return false;
        final var initializer = duplicate.getNext();
        if (!(initializer instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESPECIAL
            || !SET.equals(call.owner) || !"<init>".equals(call.name) || !"()V".equals(call.desc)) return false;
        return initializer.getNext() instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
            && EVENT.equals(field.owner) && "bars".equals(field.name) && "Ljava/util/Set;".equals(field.desc);
    }
}
