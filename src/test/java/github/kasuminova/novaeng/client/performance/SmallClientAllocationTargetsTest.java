package github.kasuminova.novaeng.client.performance;

import github.kasuminova.novaeng.common.performance.RuntimeEventAllocationTransform;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmallClientAllocationTargetsTest {
    private static ClassNode read(final String type) throws Exception {
        try (final var input = SmallClientAllocationTargetsTest.class.getResourceAsStream("/" + type + ".class")) {
            assertNotNull(input, "The supported production dependency must be present: " + type);
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    @Test
    void greenHatCreatesTenIdenticalModelsAndHasNoPerRenderGeometryMutation() throws Exception {
        final String modelName = "com/meteor/extrabotany/client/model/ModelGreenHat";
        final ClassNode layer = read("com/meteor/extrabotany/client/render/CosmeticItemRenderLayer");
        final MethodNode greenHats = layer.methods.stream().filter(method -> method.name.equals("greenHats"))
            .findFirst().orElseThrow();
        int count = 0;
        for (final var instruction : greenHats.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                && modelName.equals(type.desc)) count++;
        }
        assertEquals(10, count, "The redirect must match all ten constructors");
        final ClassNode model = read(modelName);
        for (final MethodNode method : model.methods) {
            if (!List.of("render", "setRotationAngles", "func_78087_a").contains(method.name)) continue;
            for (final var instruction : method.instructions) {
                assertNotEquals(Opcodes.PUTFIELD, instruction.getOpcode(), "Recheck reuse if animation changes");
            }
        }
    }

    @Test
    void constructorPatchMatchesActualSetFieldAndIsIdempotent() throws Exception {
        final ClassNode event = read("xyz/phanta/tconevo/client/event/ItemStackBarEvent");
        assertTrue(RuntimeEventAllocationTransform.apply(event));
        assertFalse(RuntimeEventAllocationTransform.apply(event));
        int factories = 0;
        for (final MethodNode method : event.methods) for (final var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/LazyLinkedSet")
                && call.name.equals("create")) factories++;
        }
        assertEquals(1, factories);
        assertTrue(event.fields.stream().anyMatch(field -> field.name.equals("bars")
            && field.desc.equals("Ljava/util/Set;")));

        // Load the actual patched constructor in a tiny probe: the Event super call and ItemStack
        // parameter are substituted only to avoid starting Forge/Minecraft during this verifier test.
        final MethodNode constructor = event.methods.stream().filter(method -> method.name.equals("<init>"))
            .findFirst().orElseThrow();
        final String probeName = "github/kasuminova/novaeng/client/performance/PatchedBarEventProbe";
        final ClassNode probe = new ClassNode();
        probe.version = event.version;
        probe.access = Opcodes.ACC_PUBLIC;
        probe.name = probeName;
        probe.superName = "java/lang/Object";
        probe.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "bars", "Ljava/util/Set;", null, null));
        probe.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "stack", "Ljava/lang/Object;", null, null));
        constructor.desc = "(Ljava/lang/Object;)V";
        for (final var instruction : constructor.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("<init>")
                && call.owner.equals(event.superName)) call.owner = "java/lang/Object";
            if (instruction instanceof FieldInsnNode field && field.owner.equals(event.name)) {
                field.owner = probeName;
                if (field.name.equals("stack")) field.desc = "Ljava/lang/Object;";
            }
        }
        probe.methods.add(constructor);
        final byte[] code = bytes(probe);
        final Class<?> loaded = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() { return defineClass(null, code, 0, code.length); }
        }.define();
        final Object stack = new Object();
        final Object instance = loaded.getConstructor(Object.class).newInstance(stack);
        assertInstanceOf(LazyLinkedSet.class, loaded.getField("bars").get(instance));
        assertSame(stack, loaded.getField("stack").get(instance));
    }

    @Test
    void incompatibleConstructorLeavesTheWholeClassUnchanged() throws Exception {
        final ClassNode event = read("xyz/phanta/tconevo/client/event/ItemStackBarEvent");
        event.fields.stream().filter(field -> field.name.equals("bars")).findFirst().orElseThrow()
            .desc = "Ljava/util/LinkedHashSet;";
        final byte[] before = bytes(event);
        assertFalse(RuntimeEventAllocationTransform.apply(event));
        assertArrayEquals(before, bytes(event));
    }

    @Test
    void circulationHitIsTheSameResourceKeyAndOnlyCallsTheCachedDisplayList() throws Exception {
        final ClassNode helper = read("com/circulation/circulation_networks/client/render/RotatingModelRenderHelper");
        final MethodNode draw = helper.methods.stream()
            .filter(method -> method.name.equals("renderFullBrightDisplayList")).findFirst().orElseThrow();
        int compute = 0;
        int list = 0;
        for (final var instruction : draw.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("computeIfAbsent")) compute++;
            if (instruction instanceof MethodInsnNode call && (call.name.equals("callList")
                || call.name.equals("func_179148_o"))) list++;
        }
        assertEquals(1, compute);
        assertEquals(1, list);
        assertTrue(helper.fields.stream().anyMatch(field -> field.name.equals("FULL_BRIGHT_DISPLAY_LISTS")
            && field.desc.equals("Ljava/util/Map;")));
    }

    private static byte[] bytes(final ClassNode type) {
        final ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }
}
