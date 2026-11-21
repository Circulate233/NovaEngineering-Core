package github.kasuminova.novaeng.client.moreoverlays;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * One reusable sink per renderer, with no allocation per vertex or cross.
 */
public final class LightOverlayMinecraftSink implements LightOverlayBatch.Sink {
    private BufferBuilder buffer;

    @Override
    public void begin() {
        buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    }

    @Override
    public void vertex(final double x, final double y, final double z, final float red, final float green, final float blue) {
        buffer.pos(x, y, z).color(red, green, blue, 1.0f).endVertex();
    }

    @Override
    public void end() {
        Tessellator.getInstance().draw();
    }
}
