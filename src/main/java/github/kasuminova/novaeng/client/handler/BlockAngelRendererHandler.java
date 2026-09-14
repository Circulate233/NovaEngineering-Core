package github.kasuminova.novaeng.client.handler;

import github.kasuminova.novaeng.common.block.BlockAngel;
import github.kasuminova.novaeng.common.item.ItemBlockAngel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BlockAngelRendererHandler {
    public static final BlockAngelRendererHandler INSTANCE = new BlockAngelRendererHandler();

    private BlockAngelRendererHandler() {
    }

    protected static void renderAngelBlockToWorld(final BlockPos renderPos) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = getViewEntity();

        float partialTicks = mc.getRenderPartialTicks();
        double tx = view.lastTickPosX + ((view.posX - view.lastTickPosX) * partialTicks);
        double ty = view.lastTickPosY + ((view.posY - view.lastTickPosY) * partialTicks);
        double tz = view.lastTickPosZ + ((view.posZ - view.lastTickPosZ) * partialTicks);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-tx, -ty, -tz);
            GlStateManager.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());
            GlStateManager.translate(0.125D, 0.125D, 0.125D);
            GlStateManager.scale(0.75F, 0.75F, 0.75F);

            GlStateManager.color(1F, 1F, 1F, 0.5F);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_DST_COLOR);

            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            mc.getBlockRendererDispatcher().renderBlock(
                BlockAngel.INSTANCE.getDefaultState(),
                BlockPos.ORIGIN,
                mc.world,
                buf
            );
            tess.draw();
        } finally {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }
    }

    protected static Entity getViewEntity() {
        Minecraft mc = Minecraft.getMinecraft();
        Entity rView = mc.getRenderViewEntity();
        return rView == null ? mc.player : rView;
    }

    @SubscribeEvent
    public void onRenderLast(final RenderWorldLastEvent ignored) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        WorldClient world = Minecraft.getMinecraft().world;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() && (held = player.getHeldItemOffhand()).isEmpty() || !(held.getItem() instanceof ItemBlockAngel)) {
            return;
        }
        if (world == null) {
            return;
        }

        BlockPos renderPos = new BlockPos(player.posX, player.posY + player.eyeHeight, player.posZ);
        if (!player.isSneaking()) {
            renderPos = renderPos.offset(player.getAdjustedHorizontalFacing(), 2);
        } else {
            renderPos = renderPos.add(0, -player.height, 0);
        }
        IBlockState block = world.getBlockState(renderPos);
        if (block.getBlock().isReplaceable(world, renderPos)) {
            renderAngelBlockToWorld(renderPos);
        }
    }

}
