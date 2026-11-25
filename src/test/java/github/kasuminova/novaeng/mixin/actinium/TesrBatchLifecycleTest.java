package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesrBatchLifecycleTest {
    private static final String TARGET = "com/dhj/actinium/render/terrain/ActiniumWorldRenderer";
    private static final String GUARD = "com/dhj/actinium/render/terrain/TileEntityGlStateGuard";
    private static final String DISPATCHER = "net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher";
    private static final String SCOPES = "github/kasuminova/novaeng/client/render/TileEntityBatchScopes";

    private static List<String> guardedBatcherCalls(final ClassNode owner, final MethodNode method) {
        final List<String> result = new ArrayList<>();
        for (final MethodInsnNode call : calls(method)) {
            if (call.owner.equals(owner.name) && (call.name.contains("nova$beginBatchScope")
                || call.name.contains("nova$endBatchScope") || call.name.contains("nova$drawOpenedBatchOnExit"))) {
                result.addAll(guardedBatcherCalls(owner, method(owner, call.name, call.desc)));
            } else if (call.owner.equals(GUARD)
                || call.owner.startsWith("com/gtnewhorizons/angelica/rendering/tesr/")
                || (call.owner.equals(DISPATCHER) && call.name.equals("drawBatch"))) {
                result.add(call.owner + "." + call.name + call.desc);
            }
        }
        return result;
    }

    private static List<String> callSignatures(final MethodNode method) {
        return calls(method).stream().map(call -> call.owner + "." + call.name + call.desc).toList();
    }

    private static int indexOf(final List<MethodInsnNode> calls, final String name) {
        for (int i = 0; i < calls.size(); i++) if (calls.get(i).name.equals(name)) return i;
        throw new AssertionError("Missing call " + name);
    }

    private static MethodNode handler(final ClassNode type, final String name) {
        return type.methods.stream().filter(method -> method.name.contains(name)).findFirst().orElseThrow();
    }

    private static MethodNode method(final ClassNode type, final String name, final String descriptor) {
        return type.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                           .findFirst().orElseThrow();
    }

    private static void assertRedirectCount(final MethodNode before, final MethodNode after,
                                            final String originalOwner, final String originalName,
                                            final MethodNode redirect) {
        final long originalCount = calls(before).stream()
            .filter(call -> call.owner.equals(originalOwner) && call.name.equals(originalName)).count();
        final long redirectCount = calls(after).stream()
            .filter(call -> call.owner.equals(TARGET) && call.name.equals(redirect.name) && call.desc.equals(redirect.desc)).count();
        assertTrue(originalCount > 0, "Missing original " + originalName);
        assertEquals(originalCount, redirectCount, "Every " + originalName + " call site must be redirected");
    }

    private static List<MethodInsnNode> calls(final MethodNode method) {
        final List<MethodInsnNode> calls = new ArrayList<>();
        for (final var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) calls.add(call);
        }
        return calls;
    }

    private static ClassNode dependency(final String name) throws Exception {
        try (final var input = TesrBatchLifecycleTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static ClassNode transformed() throws Exception {
        final byte[] bytes = IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin",
            new String[]{TARGET}, "actinium.MixinActiniumWorldRendererLazyBatch")[0];
        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    @Test
    void realTransformationPreservesGuardAroundAllBatchersAndExitPaths() throws Exception {
        final ClassNode original = dependency(TARGET);
        final ClassNode transformed = transformed();
        final String descriptor = "(L" + TARGET + "$TileEntityRenderContext;)I";
        final MethodNode before = method(original, "renderBlockEntities", descriptor);
        final MethodNode after = method(transformed, "renderBlockEntities", descriptor);
        final MethodNode begin = handler(transformed, "nova$beginBatchScope");
        final MethodNode end = handler(transformed, "nova$endBatchScope");
        final MethodNode defer = handler(transformed, "nova$deferBatchOpen");
        final MethodNode draw = handler(transformed, "nova$drawOpenedBatchOnExit");

        // Actinium's nested finally blocks can emit separate cleanup copies for returns and
        // exceptions. Compare to the real dependency and retain cleanup at every call site.
        assertEquals(guardedBatcherCalls(original, before), guardedBatcherCalls(transformed, after));
        assertRedirectCount(before, after, GUARD, "push", begin);
        assertRedirectCount(before, after, GUARD, "pop", end);
        assertRedirectCount(before, after, DISPATCHER, "preDrawBatch", defer);
        assertRedirectCount(before, after, DISPATCHER, "drawBatch", draw);
        assertEquals(List.of(GUARD + ".push()V", SCOPES + ".begin()V"), callSignatures(begin));
        assertEquals(List.of(SCOPES + ".end()V", GUARD + ".pop()V"), callSignatures(end));
        assertTrue(calls(after).stream().noneMatch(call -> (call.owner.equals(DISPATCHER)
            && (call.name.equals("preDrawBatch") || call.name.equals("drawBatch")))
            || (call.owner.equals(GUARD) && (call.name.equals("push") || call.name.equals("pop")))),
            "Guard and FastTESR calls must go through their scope-aware redirects");
        assertTrue(transformed.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                                      .noneMatch(instruction -> instruction instanceof TypeInsnNode type
                                          && type.desc.startsWith("org/spongepowered/asm/mixin/injection/callback/")),
            "TESR pass tracking must not allocate callback objects");
    }

    @Test
    void firstVisibleDispatcherCallOpensFastBatchAndEmptyPassDoesNotDraw() throws Exception {
        final ClassNode transformed = transformed();
        final MethodNode deferred = handler(transformed, "nova$deferBatchOpen");
        assertTrue(calls(deferred).isEmpty());
        final MethodNode render = handler(transformed, "nova$openBatchBeforeFirstRender");
        final List<MethodInsnNode> renderCalls = calls(render);
        assertTrue(indexOf(renderCalls, "shouldSkip") < indexOf(renderCalls, "ensureOpened"));
        assertTrue(indexOf(renderCalls, "ensureOpened") < indexOf(renderCalls, "render"));
        final MethodNode draw = handler(transformed, "nova$drawOpenedBatchOnExit");
        final List<MethodInsnNode> drawCalls = calls(draw);
        assertTrue(indexOf(drawCalls, "isBatchOpened") < indexOf(drawCalls, "drawBatch"));
        assertTrue(Arrays.stream(draw.instructions.toArray())
                         .anyMatch(instruction -> instruction instanceof JumpInsnNode), "Empty passes must skip drawBatch");

        final ClassNode state = dependency(SCOPES + "$State");
        final MethodNode open = method(state, "ensureOpened", "(L" + DISPATCHER + ";)V");
        assertEquals(List.of("preDrawBatch"), calls(open).stream().map(call -> call.name).toList(),
            "Opening FastTESR must not push a second GL guard inside the already active pass");
        assertTrue(Arrays.stream(open.instructions.toArray())
                         .anyMatch(instruction -> instruction instanceof JumpInsnNode), "Repeated render calls must reuse the open batch");
    }
}
