package github.kasuminova.novaeng.mixin.moreoverlays;

import at.feldim2425.moreoverlays.api.lightoverlay.ILightScanner;
import at.feldim2425.moreoverlays.config.Config;
import at.feldim2425.moreoverlays.lightoverlay.LightOverlayRenderer;
import github.kasuminova.novaeng.client.moreoverlays.LightOverlayBatch;
import github.kasuminova.novaeng.client.moreoverlays.LightOverlayMinecraftSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LightOverlayRenderer.class, remap = false)
public abstract class MixinLightOverlayRenderer {
    @Shadow
    @Final
    private static ResourceLocation BLANK_TEX;
    @Shadow
    private static RenderManager render;
    @Unique
    private LightOverlayBatch.Sink novaeng$lightSink;

    /** @author NovaEngineering @reason Emit all light-overlay crosses in one batch without per-cross objects. */
    @Overwrite
    public void renderOverlays(final ILightScanner scanner) {
        Minecraft.getMinecraft().renderEngine.bindTexture(BLANK_TEX);
        GlStateManager.pushMatrix();
        try {
            GL11.glLineWidth(Config.render_spawnLineWidth);
            GlStateManager.translate(-render.viewerPosX, -render.viewerPosY, -render.viewerPosZ);
            if (novaeng$lightSink == null) novaeng$lightSink = new LightOverlayMinecraftSink();
            LightOverlayBatch.emit(scanner, Config.render_spawnAColor, Config.render_spawnNColor, novaeng$lightSink);
        } finally {
            GlStateManager.popMatrix();
        }
    }

}
