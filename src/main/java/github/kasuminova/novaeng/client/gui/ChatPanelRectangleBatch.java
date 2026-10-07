package github.kasuminova.novaeng.client.gui;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

/** Batches the ordered, same-state rectangles emitted by Revo UI's fading chat panel. */
public final class ChatPanelRectangleBatch {
    private static final ThreadLocal<State> STATES = ThreadLocal.withInitial(State::new);

    private ChatPanelRectangleBatch() {
    }

    public static void begin() {
        final State state = STATES.get();
        if (state.depth++ == 0) {
            state.rectangleCount = 0;
        }
    }

    public static void add(final int x1, final int y1, final int x2, final int y2, final int color) {
        final State state = STATES.get();
        if (state.depth == 0) {
            return;
        }
        final int offset = state.rectangleCount * 5;
        state.ensureCapacity(offset + 5);
        state.rectangles[offset] = x1;
        state.rectangles[offset + 1] = y1;
        state.rectangles[offset + 2] = x2;
        state.rectangles[offset + 3] = y2;
        state.rectangles[offset + 4] = color;
        state.rectangleCount++;
    }

    public static void end() {
        final State state = STATES.get();
        if (state.depth == 0 || --state.depth != 0) {
            return;
        }
        final int rectangleCount = state.rectangleCount;
        state.rectangleCount = 0;
        if (rectangleCount == 0) {
            return;
        }

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO
        );
        try {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int index = 0, offset = 0; index < rectangleCount; index++, offset += 5) {
                final int x1 = state.rectangles[offset];
                final int y1 = state.rectangles[offset + 1];
                final int x2 = state.rectangles[offset + 2];
                final int y2 = state.rectangles[offset + 3];
                final int color = state.rectangles[offset + 4];
                final int red = color >> 16 & 255;
                final int green = color >> 8 & 255;
                final int blue = color & 255;
                final int alpha = color >>> 24;

                buffer.pos(x1, y2, 0.0D).color(red, green, blue, alpha).endVertex();
                buffer.pos(x2, y2, 0.0D).color(red, green, blue, alpha).endVertex();
                buffer.pos(x2, y1, 0.0D).color(red, green, blue, alpha).endVertex();
                buffer.pos(x1, y1, 0.0D).color(red, green, blue, alpha).endVertex();
            }
            tessellator.draw();
        } finally {
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }
    }

    private static final class State {
        private int[] rectangles = new int[5 * 32];
        private int rectangleCount;
        private int depth;

        private void ensureCapacity(final int required) {
            if (required > this.rectangles.length) {
                this.rectangles = Arrays.copyOf(this.rectangles, Math.max(required, this.rectangles.length << 1));
            }
        }
    }
}
