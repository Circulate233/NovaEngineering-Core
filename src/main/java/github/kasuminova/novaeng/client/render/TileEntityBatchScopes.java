package github.kasuminova.novaeng.client.render;

import com.dhj.actinium.render.terrain.TileEntityGlStateGuard;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

/**
 * Reuses the small per-pass bookkeeping objects while preserving nested render scopes.
 */
public final class TileEntityBatchScopes {
    private static final ThreadLocal<Stack> STACKS = ThreadLocal.withInitial(Stack::new);

    private TileEntityBatchScopes() {
    }

    public static void begin() {
        final Stack stack = STACKS.get();
        if (stack.depth == stack.states.size()) {
            stack.states.add(new State());
        }
        stack.depth++;
    }

    public static void end() {
        final Stack stack = STACKS.get();
        final State state = stack.states.get(--stack.depth);
        state.guardPushed = false;
        state.batchOpened = false;
    }

    public static State current() {
        final Stack stack = STACKS.get();
        return stack.depth == 0 ? null : stack.states.get(stack.depth - 1);
    }

    public static final class State {
        private boolean guardPushed;
        private boolean batchOpened;

        public boolean isGuardPushed() {
            return guardPushed;
        }

        public boolean isBatchOpened() {
            return batchOpened;
        }

        public void ensureOpened(final TileEntityRendererDispatcher dispatcher) {
            if (!guardPushed) {
                TileEntityGlStateGuard.push();
                guardPushed = true;
            }
            if (!batchOpened) {
                dispatcher.preDrawBatch();
                batchOpened = true;
            }
        }
    }

    private static final class Stack {
        private final ObjectArrayList<State> states = new ObjectArrayList<>(2);
        private int depth;
    }
}
