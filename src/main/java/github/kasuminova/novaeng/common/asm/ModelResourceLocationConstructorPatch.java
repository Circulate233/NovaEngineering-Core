package github.kasuminova.novaeng.common.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Changes the delegating constructor; Mixin injectors cannot change invokespecial's descriptor.
 */
public final class ModelResourceLocationConstructorPatch {
    private static final String RESOURCE_LOCATION = "net/minecraft/util/ResourceLocation";
    private static final String INPUT = "(L" + RESOURCE_LOCATION + ";Ljava/lang/String;)V";

    private ModelResourceLocationConstructorPatch() {
    }

    public static boolean apply(final ClassNode target) {
        for (int i = 0; i < target.methods.size(); i++) {
            final MethodNode method = target.methods.get(i);
            if (!method.name.equals("<init>") || !method.desc.equals(INPUT)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode delegate) || delegate.getOpcode() != Opcodes.INVOKESPECIAL
                    || !delegate.owner.equals(target.name) || !delegate.name.equals("<init>")
                    || !delegate.desc.equals("(Ljava/lang/String;Ljava/lang/String;)V")) {
                    continue;
                }
                final AbstractInsnNode variant = previous(delegate);
                final AbstractInsnNode toString = previous(variant);
                final AbstractInsnNode location = previous(toString);
                final AbstractInsnNode receiver = previous(location);
                if (!loads(variant, 2) || !loads(location, 1) || !loads(receiver, 0)
                    || !(toString instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !call.owner.equals(RESOURCE_LOCATION) || !call.name.equals("toString")
                    || !call.desc.equals("()Ljava/lang/String;")) {
                    continue; // Do not erase another transformer's constructor logic.
                }
                final InsnList replacement = new InsnList();
                replacement.add(new InsnNode(Opcodes.ICONST_0));
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
                replacement.add(new VarInsnNode(Opcodes.ALOAD, 2));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "github/kasuminova/novaeng/common/resource/ModelResourceLocationParts", "from",
                    "(L" + RESOURCE_LOCATION + ";Ljava/lang/String;)[Ljava/lang/String;", false));
                method.instructions.insert(receiver, replacement);
                method.instructions.remove(location);
                method.instructions.remove(toString);
                method.instructions.remove(variant);
                delegate.desc = "(I[Ljava/lang/String;)V";
                method.maxStack = Math.max(method.maxStack, 4);
                return true;
            }
        }
        return false;
    }

    private static boolean loads(final AbstractInsnNode instruction, final int slot) {
        return instruction instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.ALOAD && variable.var == slot;
    }

    private static AbstractInsnNode previous(final AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }
}
