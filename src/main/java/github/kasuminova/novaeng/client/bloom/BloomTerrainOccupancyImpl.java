package github.kasuminova.novaeng.client.bloom;

import com.dhj.actinium.render.terrain.ActiniumWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.util.BlockRenderLayer;

import java.util.Collection;
import java.util.Map;

/**
 * Conservative accessor-chain implementation of {@link BloomTerrainOccupancy}.
 *
 * <p>Every unknown renderer, manager, configuration, mapping, list, or pass returns non-empty so
 * the original bloom path remains responsible for rendering.</p>
 */
public final class BloomTerrainOccupancyImpl implements BloomTerrainOccupancy {

    private static final BloomTerrainOccupancy INSTANCE = new BloomTerrainOccupancyImpl();

    private BloomTerrainOccupancyImpl() {
    }

    /** Returns the shared stateless occupancy query. */
    public static BloomTerrainOccupancy instance() {
        return INSTANCE;
    }

    @Override
    public boolean isDefinitelyEmpty(final BlockRenderLayer bloomLayer) {
        if (bloomLayer == null) {
            return false;
        }
        final ActiniumWorldRenderer renderer = ActiniumWorldRenderer.instanceNullable();
        if (renderer == null) {
            return false;
        }
        final RenderPassConfiguration<?> configuration = renderer.getRenderPassConfiguration();
        final RenderSectionManager manager = renderer.getRenderSectionManager();
        if (configuration == null || manager == null) {
            return false;
        }
        final SortedRenderLists lists = manager.getRenderLists();
        if (lists == null) {
            return false;
        }

        final Map<?, ?> mappings = configuration.vanillaRenderStages();
        if (mappings == null || !mappings.containsKey(bloomLayer)) {
            return false;
        }
        final Object mapped = mappings.get(bloomLayer);
        if (!(mapped instanceof Collection<?> passes) || passes.isEmpty()) {
            return false;
        }
        for (final Object pass : passes) {
            if (pass == null || lists.hasPass((TerrainRenderPass) pass)) {
                return false;
            }
        }
        return true;
    }
}
