package github.kasuminova.novaeng.mixin.botania;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.model.ModelSpinningCubes;

@Mixin(value = ModelSpinningCubes.class, remap = false)
public abstract class MixinModelSpinningCubes {

    @Shadow
    @Final
    ModelRenderer spinningCube;

    /**
     * @author circulation
     * @reason Hoist time-only trigonometry out of the per-cube loop.
     */
    @Overwrite
    public void renderSpinningCubes(final int cubes, final int repeat, final int origRepeat) {
        GlStateManager.disableTexture2D();
        final double ticks = (double) ((float) ClientTickHandler.ticksInGame + ClientTickHandler.partialTicks)
                - 1.3D * (double) (origRepeat - repeat);
        final float offsetPerCube = (float) (360 / cubes);
        final float radiusX = (float) ((double) 0.35F + (double) 0.05F * Math.sin(ticks / (double) 6.0F));
        final float radiusZ = (float) ((double) 0.35F + (double) 0.05F * Math.cos(ticks / (double) 6.0F));
        final float xRotate = (float) Math.sin(ticks * (double) 0.2F) / 2.0F;
        final float yRotate = (float) Math.max(0.6D, Math.sin(ticks * (double) 0.1F) / 2.0D + 0.5D);
        final float zRotate = (float) Math.cos(ticks * (double) 0.2F) / 2.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.025F, 0.85F, -0.025F);
        for (int i = 0; i < cubes; i++) {
            final float offset = offsetPerCube * (float) i;
            final float deg = (float) ((int) (ticks / (double) 0.2F % (double) 360.0F + (double) offset));
            final float rad = deg * (float) Math.PI / 180.0F;
            final float x = (float) ((double) radiusX * Math.cos(rad));
            final float z = (float) ((double) radiusZ * Math.sin(rad));
            final float y = (float) Math.cos((ticks + (double) (50 * i)) / 5.0D) / 10.0F;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(deg, xRotate, yRotate, zRotate);
            if (repeat < origRepeat) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, (float) repeat / (float) origRepeat * 0.4F);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(770, 771);
                GlStateManager.disableAlpha();
            } else {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            final int light = 15728880;
            final int lightmapX = light % 65536;
            final int lightmapY = light / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightmapX, (float) lightmapY);
            this.spinningCube.render(0.0625F);
            if (repeat < origRepeat) {
                GlStateManager.disableBlend();
                GlStateManager.enableAlpha();
            }
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        if (repeat != 0) {
            this.renderSpinningCubes(cubes, repeat - 1, origRepeat);
        }
    }
}
