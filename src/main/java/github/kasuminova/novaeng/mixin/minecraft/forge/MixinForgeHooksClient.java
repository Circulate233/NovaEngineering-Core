package github.kasuminova.novaeng.mixin.minecraft.forge;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class MixinForgeHooksClient {

    /**
     * @author circulation
     * @reason 防止出现越界错误
     */
    @Overwrite
    public static void putQuadColor(BufferBuilder renderer, BakedQuad quad, int color) {
        float cb = color & 0xFF;
        float cg = (color >>> 8) & 0xFF;
        float cr = (color >>> 16) & 0xFF;
        float ca = (color >>> 24) & 0xFF;
        VertexFormat format = quad.getFormat();
        int size = format.getIntegerSize();
        int offset = format.getColorOffset() / 4; // assumes that color is aligned
        boolean hasColor = format.hasColor();
        int[] words = hasColor ? quad.getVertexData() : null;
        for (int i = 0; i < 4; i++) {
            int vc = hasColor && words.length > offset + size * i ? words[offset + size * i] : 0xFFFFFFFF;
            float vcr = vc & 0xFF;
            float vcg = (vc >>> 8) & 0xFF;
            float vcb = (vc >>> 16) & 0xFF;
            float vca = (vc >>> 24) & 0xFF;
            int ncr = Math.min(0xFF, (int) (cr * vcr / 0xFF));
            int ncg = Math.min(0xFF, (int) (cg * vcg / 0xFF));
            int ncb = Math.min(0xFF, (int) (cb * vcb / 0xFF));
            int nca = Math.min(0xFF, (int) (ca * vca / 0xFF));
            renderer.putColorRGBA(renderer.getColorIndex(4 - i), ncr, ncg, ncb, nca);
        }
    }
}
