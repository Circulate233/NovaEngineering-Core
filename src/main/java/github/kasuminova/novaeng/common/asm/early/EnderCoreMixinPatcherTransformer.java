package github.kasuminova.novaeng.common.asm.early;

import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;

/**
 * Skips EnderCore's full ASM visit when neither a direct target nor a direct interface matches.
 */
public final class EnderCoreMixinPatcherTransformer implements IExplicitTransformer {
    public static final String TARGET = "com.enderio.core.common.transform.SimpleMixinPatcher";
    private static final String TARGET_INTERNAL = TARGET.replace('.', '/');
    private static final String MULTIMAP = "com/google/common/collect/Multimap";
    private static final String INTERFACES = "L" + MULTIMAP + ";";

    /**
     * Called after EnderCore has collected direct targets and captured source classes.
     */
    public static boolean hasInterfaceTarget(final byte[] bytes, final Multimap<?, ?> interfaceTargets) {
        for (final String name : new ClassReader(bytes).getInterfaces()) {
            if (interfaceTargets.containsKey(name)) return true;
        }
        return false;
    }

    private static int directTargetLocal(final TypeInsnNode allocation) {
        final var ret = previous(allocation);
        final var bytes = previous(ret);
        final var noInterfaces = previous(bytes);
        final var interfacesEmpty = previous(noInterfaces);
        final var interfaces = previous(interfacesEmpty);
        final var receiver = previous(interfaces);
        final var hasTargets = previous(receiver);
        final var targetsEmpty = previous(hasTargets);
        final var targets = previous(targetsEmpty);
        if (ret == null || ret.getOpcode() != Opcodes.ARETURN
            || !(bytes instanceof VarInsnNode input) || input.getOpcode() != Opcodes.ALOAD || input.var != 3
            || !jumpsTo(noInterfaces, Opcodes.IFEQ, allocation) || !isEmptyCall(interfacesEmpty, MULTIMAP)
            || !(interfaces instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.GETFIELD
            || !field.owner.equals(TARGET_INTERNAL) || !field.name.equals("interfaceTargets") || !field.desc.equals(INTERFACES)
            || !(receiver instanceof VarInsnNode self) || self.getOpcode() != Opcodes.ALOAD || self.var != 0
            || !jumpsTo(hasTargets, Opcodes.IFEQ, allocation) || !isEmptyCall(targetsEmpty, "java/util/List")
            || !(targets instanceof VarInsnNode list) || list.getOpcode() != Opcodes.ALOAD) return -1;
        return list.var;
    }

    private static AbstractInsnNode previous(final AbstractInsnNode node) {
        AbstractInsnNode cursor = node == null ? null : node.getPrevious();
        while (cursor != null && cursor.getOpcode() < 0) cursor = cursor.getPrevious();
        return cursor;
    }

    private static boolean jumpsTo(final AbstractInsnNode instruction, final int opcode, final TypeInsnNode allocation) {
        if (!(instruction instanceof JumpInsnNode jump) || jump.getOpcode() != opcode) return false;
        AbstractInsnNode target = jump.label;
        while (target != null && target.getOpcode() < 0) target = target.getNext();
        return target == allocation;
    }

    private static boolean isEmptyCall(final AbstractInsnNode instruction, final String owner) {
        return instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE
            && call.owner.equals(owner) && call.name.equals("isEmpty") && call.desc.equals("()Z");
    }

    @Override
    public byte[] transform(final byte[] bytes) {
        final ClassReader reader = new ClassReader(bytes);
        if (!TARGET_INTERNAL.equals(reader.getClassName())) return bytes;
        final ClassNode node = new ClassNode();
        reader.accept(node, 0);
        for (final var method : node.methods) {
            if (!method.name.equals("transform") || !method.desc.equals("(Ljava/lang/String;Ljava/lang/String;[B)[B"))
                continue;
            for (final var instruction : method.instructions) {
                if (!(instruction instanceof TypeInsnNode allocation) || allocation.getOpcode() != Opcodes.NEW
                    || !allocation.desc.equals("org/objectweb/asm/tree/ClassNode")) continue;
                final int targets = directTargetLocal(allocation);
                if (targets < 0) continue;

                final LabelNode parse = new LabelNode();
                final InsnList guard = new InsnList();
                guard.add(new VarInsnNode(Opcodes.ALOAD, targets));
                guard.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, parse));
                guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
                guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
                guard.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL, "interfaceTargets", INTERFACES));
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "github/kasuminova/novaeng/common/asm/early/EnderCoreMixinPatcherTransformer",
                    "hasInterfaceTarget", "([B" + INTERFACES + ")Z", false));
                guard.add(new JumpInsnNode(Opcodes.IFNE, parse));
                guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
                guard.add(new InsnNode(Opcodes.ARETURN));
                guard.add(parse);
                guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insertBefore(allocation, guard);

                final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                LogManager.getLogger("NOVAENG_CORE_PRE").info("Applied EnderCore class header fast path");
                return writer.toByteArray();
            }
        }
        LogManager.getLogger("NOVAENG_CORE_PRE").warn("EnderCore transformer shape changed; keeping the original transformer");
        return bytes;
    }
}
