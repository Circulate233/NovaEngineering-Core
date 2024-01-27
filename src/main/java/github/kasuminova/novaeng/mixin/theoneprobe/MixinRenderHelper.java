package github.kasuminova.novaeng.mixin.theoneprobe;

import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.rendering.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RenderHelper.class)
public class MixinRenderHelper {

    /**
     * @author Kasumi_Nova
     * @reason 防止渲染实体后头部乱动。
     */
    @Overwrite(remap = false)
    public static void renderEntity(final Entity entity, final int xPos, final int yPos, final float scale) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)(xPos + 8), (float)(yPos + 24), 50.0F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(RenderHelper.rot, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, (float) entity.getYOffset(), 0.0F);
        Minecraft.getMinecraft().getRenderManager().playerViewY = 180.0F;
        float prevPitch = entity.rotationPitch;
        entity.rotationPitch = 0.0F;

        try {
            Minecraft.getMinecraft().getRenderManager().renderEntity(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, false);
        } catch (Exception var5) {
            TheOneProbe.logger.error("Error rendering entity!", var5);
        }

        entity.rotationPitch = prevPitch;
        GlStateManager.popMatrix();
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
        GlStateManager.disableColorMaterial();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

}
