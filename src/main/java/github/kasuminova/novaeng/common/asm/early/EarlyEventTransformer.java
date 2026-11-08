package github.kasuminova.novaeng.common.asm.early;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import top.outlands.foundation.IExplicitTransformer;

/** Patches the excluded Forge transformer classes before their first definition. */
public final class EarlyEventTransformer implements IExplicitTransformer {
    public static final String SUBSCRIBER = "net.minecraftforge.fml.common.asm.transformers.EventSubscriberTransformer";
    public static final String SUBSCRIPTION = "net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer";
    private static final String HELPER = "github/kasuminova/novaeng/common/asm/early/EventClassScan";
    private static final String TRANSFORM = "(Ljava/lang/String;Ljava/lang/String;[B)[B";

    @Override
    public byte[] transform(final byte[] bytes) {
        final ClassReader reader = new ClassReader(bytes);
        final ClassNode node = new ClassNode();
        reader.accept(node, 0);
        final boolean subscriber = node.name.equals(SUBSCRIBER.replace('.', '/'));
        if (!subscriber && !node.name.equals(SUBSCRIPTION.replace('.', '/'))) return bytes;
        int changed = 0;
        for (final var method : node.methods) {
            if (!method.name.equals("transform") || !method.desc.equals(TRANSFORM)) continue;
            if (subscriber) {
                final InsnList guard = new InsnList();
                final LabelNode original = new LabelNode();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
                guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "maySubscribe", "([B)Z", false));
                guard.add(new JumpInsnNode(Opcodes.IFNE, original));
                guard.add(new VarInsnNode(Opcodes.ALOAD, 3));
                guard.add(new InsnNode(Opcodes.ARETURN));
                guard.add(original);
                guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(guard);
                changed++;
            } else {
                for (final var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.owner.equals("org/objectweb/asm/ClassReader") && call.name.equals("getSuperName")
                        && call.desc.equals("()Ljava/lang/String;")) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = HELPER;
                        call.name = "eventSuperclass";
                        call.desc = "(Lorg/objectweb/asm/ClassReader;)Ljava/lang/String;";
                        changed++;
                    }
                }
            }
        }
        if (changed != 1) {
            LogManager.getLogger("NOVAENG_CORE_PRE").warn("Early event patch shape changed for {}; keeping original transformer", node.name);
            return bytes;
        }
        // Preserve existing frames; neither patch changes local types or needs hierarchy resolution here.
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        LogManager.getLogger("NOVAENG_CORE_PRE").info("Applied early event fast path to {}", node.name);
        return writer.toByteArray();
    }
}
