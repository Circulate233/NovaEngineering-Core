package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.MixinDecisions;
import net.coderbot.iris.apiimpl.IrisApiV0Impl;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only the vanilla fullscreen quad draw with a framebuffer blit.
 *
 * <p>The method itself is deliberately never cancelled. Actinium/Iris installs several
 * injections in {@code framebufferRenderExt}, including callbacks immediately before and after
 * the tessellator draw. Replacing the draw invocation leaves the whole method and all of those
 * callbacks reachable while still avoiding the four-vertex textured quad when the fast path is
 * safe. The explicit 1100 priority lets Actinium's default-priority setup callbacks be merged
 * before Nova's argument-capture callback.</p>
 */
@Mixin(value = Framebuffer.class, priority = 1100)
public abstract class MixinFramebufferBlit {
    @Shadow
    public int framebufferObject;
    @Shadow
    public int framebufferWidth;
    @Shadow
    public int framebufferHeight;

    @Unique
    private int nova$blitWidth;
    @Unique
    private int nova$blitHeight;
    @Unique
    private boolean nova$blitDisableBlend;

    @Inject(method = "framebufferRenderExt(IIZ)V", at = @At("HEAD"))
    private void nova$captureRenderArguments(final int width, final int height, final boolean disableBlend,
                                             final CallbackInfo ci) {
        this.nova$blitWidth = width;
        this.nova$blitHeight = height;
        this.nova$blitDisableBlend = disableBlend;
    }

    @Redirect(
        method = "framebufferRenderExt(IIZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")
    )
    private void nova$blitFramebuffer(final Tessellator tessellator) {
        if (!this.nova$canBlit()) {
            tessellator.draw();
            return;
        }

        // The vanilla call also closes the BufferBuilder. Keep that lifecycle intact without
        // sending the four temporary vertices through the GL vertex uploader.
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.finishDrawing();

        final ContextCapabilities capabilities = GLContext.getCapabilities();
        final int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.framebufferObject);
            // Vanilla draws into whichever framebuffer was current on entry. Preserve that
            // destination instead of assuming the default framebuffer is bound.
            OpenGlHelper.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            if (capabilities.OpenGL30) {
                GL30.glBlitFramebuffer(
                    0, 0, this.framebufferWidth, this.framebufferHeight,
                    0, 0, this.nova$blitWidth, this.nova$blitHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
                );
            } else {
                ARBFramebufferObject.glBlitFramebuffer(
                    0, 0, this.framebufferWidth, this.framebufferHeight,
                    0, 0, this.nova$blitWidth, this.nova$blitHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
                );
            }
        } finally {
            OpenGlHelper.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            OpenGlHelper.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            buffer.reset();
        }
    }

    @Unique
    private boolean nova$canBlit() {
        if (!this.nova$blitDisableBlend
            || (MixinDecisions.actiniumLoaded && IrisApiV0Impl.INSTANCE.isShaderPackInUse())
            || !OpenGlHelper.isFramebufferEnabled() || this.framebufferObject < 0
            || this.framebufferWidth <= 0 || this.framebufferHeight <= 0
            || this.nova$blitWidth <= 0 || this.nova$blitHeight <= 0) {
            return false;
        }

        final ContextCapabilities capabilities = GLContext.getCapabilities();
        return capabilities.OpenGL30 || capabilities.GL_ARB_framebuffer_object;
    }

}
