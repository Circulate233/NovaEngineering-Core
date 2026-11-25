package github.kasuminova.novaeng.client.render.construction;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;

final class ConstructionGlState implements AutoCloseable {
    private final Arena memory = Arena.ofConfined();
    private final FloatBuffer values = memory.allocate(ValueLayout.JAVA_FLOAT, 16).asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final DoubleBuffer depthRange = memory.allocate(ValueLayout.JAVA_DOUBLE, 2).asByteBuffer().order(ByteOrder.nativeOrder()).asDoubleBuffer();
    private double nearDepth, farDepth;
    private final boolean programmable;
    private final boolean isolatedArrays;
    private int activeTexture, arrayBuffer, matrixMode, program, vertexArray;
    private int depthFunc, blendSrc, blendDst, alphaSrc, alphaDst;
    private boolean atlasTexture, lightmapTexture, lighting, cull, blend, depth, depthMask, alpha, polygonOffset, fog;
    private float factor, units, lineWidth, red, green, blue, opacity;

    ConstructionGlState(final boolean actinium) {
        final var capabilities = GLContext.getCapabilities();
        programmable = capabilities.OpenGL20;
        isolatedArrays = actinium && capabilities.OpenGL30;
    }

    private static boolean enabled(final int parameter) {
        return GlStateManager.glGetInteger(parameter) != 0;
    }

    void captureBuffer() {
        if (OpenGlHelper.vboSupported) arrayBuffer = GlStateManager.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    }

    void restoreBuffer() {
        if (OpenGlHelper.vboSupported) OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, arrayBuffer);
    }

    void capture() {
        depthRange.clear();
        GL11.glGetDouble(GL11.GL_DEPTH_RANGE, depthRange);
        nearDepth = depthRange.get(0);
        farDepth = depthRange.get(1);
        values.clear();
        GlStateManager.getFloat(GL11.GL_CURRENT_COLOR, values);
        red = values.get(0);
        green = values.get(1);
        blue = values.get(2);
        opacity = values.get(3);
        activeTexture = GlStateManager.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        matrixMode = GlStateManager.glGetInteger(GL11.GL_MATRIX_MODE);
        if (programmable) program = GlStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        lighting = enabled(GL11.GL_LIGHTING);
        cull = enabled(GL11.GL_CULL_FACE);
        blend = enabled(GL11.GL_BLEND);
        depth = enabled(GL11.GL_DEPTH_TEST);
        depthMask = enabled(GL11.GL_DEPTH_WRITEMASK);
        alpha = enabled(GL11.GL_ALPHA_TEST);
        polygonOffset = enabled(GL11.GL_POLYGON_OFFSET_FILL);
        fog = enabled(GL11.GL_FOG);
        depthFunc = GlStateManager.glGetInteger(GL11.GL_DEPTH_FUNC);
        blendSrc = GlStateManager.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDst = GlStateManager.glGetInteger(GL14.GL_BLEND_DST_RGB);
        alphaSrc = GlStateManager.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        alphaDst = GlStateManager.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        factor = getFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
        units = getFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        lineWidth = getFloat(GL11.GL_LINE_WIDTH);

        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        try {
            lightmapTexture = enabled(GL11.GL_TEXTURE_2D);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            atlasTexture = enabled(GL11.GL_TEXTURE_2D);
        } finally {
            GlStateManager.setActiveTexture(activeTexture);
        }
    }

    void prepare() {
        if (program != 0) GL20.glUseProgram(0);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disablePolygonOffset();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.glLineWidth(1.5F);
    }

    void prepareWireDepth() {
        // Polygon offset does not apply to GL_LINES; keep coplanar outlines ahead of the block surface.
        GL11.glDepthRange(nearDepth, farDepth - (farDepth - nearDepth) * 1.0e-6);
    }

    void prepareArrays() {
        if (isolatedArrays) {
            // A generic attribute on the caller's VAO can override a legacy position array.
            if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vertexArray);
            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            return;
        }
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }

    private float getFloat(final int parameter) {
        values.clear();
        GlStateManager.getFloat(parameter, values);
        return values.get(0);
    }

    void restore() {
        GL11.glDepthRange(nearDepth, farDepth);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if (atlasTexture) GlStateManager.enableTexture2D();
        else GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        if (lightmapTexture) GlStateManager.enableTexture2D();
        else GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(activeTexture);
        GlStateManager.matrixMode(matrixMode);
        if (lighting) GlStateManager.enableLighting();
        else GlStateManager.disableLighting();
        if (cull) GlStateManager.enableCull();
        else GlStateManager.disableCull();
        if (blend) GlStateManager.enableBlend();
        else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha();
        else GlStateManager.disableAlpha();
        if (depth) GlStateManager.enableDepth();
        else GlStateManager.disableDepth();
        if (fog) GlStateManager.enableFog();
        else GlStateManager.disableFog();
        GlStateManager.depthFunc(depthFunc);
        GlStateManager.depthMask(depthMask);
        GlStateManager.tryBlendFuncSeparate(blendSrc, blendDst, alphaSrc, alphaDst);
        if (polygonOffset) GlStateManager.enablePolygonOffset();
        else GlStateManager.disablePolygonOffset();
        GlStateManager.doPolygonOffset(factor, units);
        GlStateManager.glLineWidth(lineWidth);
        GlStateManager.resetColor();
        GlStateManager.color(red, green, blue, opacity);
        if (program != 0) GL20.glUseProgram(program);
    }

    @Override
    public void close() {
        if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
        memory.close();
    }
}
