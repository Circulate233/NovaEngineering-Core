package github.kasuminova.novaeng.mixin.enderutilities;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.util.Annotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullifierMixinTargetsTest {
    private static final String ORIGINAL = "fi/dy/masa/enderutilities/client/renderer/model/ModelNullifierBaked";
    private static final String MIXINS = "github/kasuminova/novaeng/mixin/enderutilities/";

    private static void assertOverwrite(final ClassNode mixin, final String selector, final String descriptor) {
        final var method = mixin.methods.stream().filter(candidate -> candidate.name.equals(selector)
            && candidate.desc.equals(descriptor)).findFirst().orElseThrow();
        assertNotNull(Annotations.getVisible(method, Overwrite.class));
        assertTrue((method.access & Opcodes.ACC_PUBLIC) != 0);
    }

    private static ClassNode read(final String name) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var input = NullifierMixinTargetsTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    void installedPrivateModelBakeIsReplacedBeforeTheLegacyModelCanBeConstructed() throws Exception {
        final ClassNode original = read(ORIGINAL + "$ModelNullifier");
        final String desc = "(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;"
            + "Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;";
        final var bake = original.methods.stream().filter(method -> method.name.equals("bake") && method.desc.equals(desc)).toList();
        assertEquals(1, bake.size());
        assertEquals(Opcodes.ACC_PUBLIC, bake.getFirst().access & Opcodes.ACC_PUBLIC);
        final ClassNode mixin = read(MIXINS + "MixinNullifierModelBake");
        assertEquals(List.of(ORIGINAL.replace('/', '.') + "$ModelNullifier"),
            Annotations.<List<String>>getValue(Annotations.getInvisible(mixin, Mixin.class), "targets"));
        assertOverwrite(mixin, "bake", desc);
        assertTrue(mixin.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                                .anyMatch(instruction -> instruction instanceof MethodInsnNode call && call.name.equals("bake")
                                    && call.owner.equals("github/kasuminova/novaeng/client/enderutilities/NullifierModelFactory")));
    }

    @Test
    void modelEventAssignmentIsInterceptedBeforeTheLoaderIsStored() throws Exception {
        final ClassNode original = read(ORIGINAL);
        final var handler = original.methods.stream().filter(method -> method.name.equals("onModelbakeEvent")
            && method.desc.equals("(Lnet/minecraftforge/client/event/ModelBakeEvent;)V")).findFirst().orElseThrow();
        assertTrue((handler.access & Opcodes.ACC_STATIC) != 0);
        assertTrue(Arrays.stream(handler.instructions.toArray()).anyMatch(instruction ->
            instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC && field.name.equals("MODEL_LOADER")));
        final ClassNode mixin = read(MIXINS + "MixinNullifierModelReloadLifecycle");
        assertOverwrite(mixin, "onModelbakeEvent", handler.desc);
        final var replacement = mixin.methods.stream().filter(method -> method.name.equals("onModelbakeEvent"))
                                             .findFirst().orElseThrow();
        assertNotNull(Annotations.getVisible(replacement, net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class));
        int released = 0;
        for (final var instruction : replacement.instructions) {
            if (instruction instanceof MethodInsnNode call) assertNotEquals("getModelLoader", call.name);
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC) {
                assertEquals(Opcodes.ACONST_NULL, field.getPrevious().getOpcode());
                released++;
            }
        }
        assertEquals(4, released, "Release loader, unbaked fallback and both reflected registry tables");
    }
}
