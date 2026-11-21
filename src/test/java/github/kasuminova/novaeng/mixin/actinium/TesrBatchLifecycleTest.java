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
            if (call.name.contains("nova$beginBatchScope") || call.name.contains("nova$endBatchScope")) {
                result.addAll(guardedBatcherCalls(owner, method(owner, call.name, call.desc)));
            } else if (call.owner.equals(GUARD)
                || call.owner.startsWith("com/gtnewhorizons/angelica/rendering/tesr/")) {
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
    void realTransformationPreservesGuardAroundAllNewBatchersAndBothExitPaths() throws Exception {
        final ClassNode original = dependency(TARGET);
        final ClassNode transformed = transformed();
        final String descriptor = "(L" + TARGET + "$TileEntityRenderContext;)I";
        final MethodNode before = method(original, "renderBlockEntities", descriptor);
        final MethodNode after = method(transformed, "renderBlockEntities", descriptor);
        assertEquals(List.of("push", "restoreForBatch", "pop", "restoreForBatch", "pop"),
            calls(before).stream().filter(call -> call.owner.equals(GUARD)).map(call -> call.name).toList());

        // The guard must also cover beginPass/compileInstancedVariants and the extra flushes when
        // every TileEntity is culled. Only the vanilla FastTESR batch is allowed to be lazy.
        assertEquals(guardedBatcherCalls(original, before), guardedBatcherCalls(transformed, after));
        assertEquals(1, calls(after).stream().filter(call -> call.name.contains("nova$beginBatchScope")).count());
        assertEquals(2, calls(after).stream().filter(call -> call.name.contains("nova$endBatchScope")).count());
        assertEquals(List.of(GUARD + ".push()V", SCOPES + ".begin()V"),
            callSignatures(handler(transformed, "nova$beginBatchScope")));
        assertEquals(List.of(SCOPES + ".end()V", GUARD + ".pop()V"),
            callSignatures(handler(transformed, "nova$endBatchScope")));
        assertEquals(0, calls(after).stream().filter(call -> call.owner.equals(DISPATCHER)
            && (call.name.equals("preDrawBatch") || call.name.equals("drawBatch"))).count());
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
