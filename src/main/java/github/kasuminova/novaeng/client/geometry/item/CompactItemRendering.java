package github.kasuminova.novaeng.client.geometry.item;

import com.dhj.actinium.config.ActiniumRuntimeOptions;
import com.dhj.actinium.render.FastLitItemDisplayListCache;
import com.dhj.actinium.render.ItemVertexAlphaOverrides;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Owned ITEM rendering; optional Actinium classes load only inside the guarded nested bridge.
 */
public final class CompactItemRendering {
    private CompactItemRendering() {
    }

    public static boolean renderIfSupported(final RenderItem renderer, final IBakedModel model,
                                            final int color, final ItemStack stack) {
        if (!(model instanceof CompactItemModel item) || !item.novaeng$isItemFormat()
            || item.novaeng$isGeometryExposed()) return false;
        if (MixinDecisions.actiniumLoaded && !Actinium.canRender()) return false;
        final List<BakedQuad> quads = item.novaeng$readQuads(null, null, 0);
        if (!quads.isEmpty() && !(MixinDecisions.actiniumLoaded && Actinium.renderCached(renderer, model, quads, color, stack))) {
            final Tessellator tessellator = Tessellator.getInstance();
            final BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.ITEM);
            renderer.renderQuads(buffer, quads, color, stack);
            tessellator.draw();
        }
        return true;
    }

    private static final class Actinium {
        static boolean canRender() {
            return ActiniumRuntimeOptions.useFastLitItemRendering()
                && !IrisApi.getInstance().isShaderPackInUse();
        }

        static boolean renderCached(final RenderItem renderer, final IBakedModel model, final List<BakedQuad> quads,
                                    final int color, final ItemStack stack) {
            if (ItemVertexAlphaOverrides.isActive()
                || !ActiniumRuntimeOptions.useFastLitItemDisplayLists()) return false;
            final FastLitItemDisplayListCache.CachedDisplayList cached =
                FastLitItemDisplayListCache.getOrCompile(renderer, model, quads, color, stack);
            if (cached == null) return false;
            cached.render();
            return true;
        }
    }
}
