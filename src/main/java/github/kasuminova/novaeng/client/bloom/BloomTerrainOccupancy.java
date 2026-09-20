package github.kasuminova.novaeng.client.bloom;

import net.minecraft.util.BlockRenderLayer;

/**
 * Determines whether the current Actinium main or shadow render list has no terrain geometry for
 * Lumenized's bloom layer.
 */
public interface BloomTerrainOccupancy {

    /**
     * Returns an authoritative empty result only when every mapped terrain pass is absent.
     *
     * @param bloomLayer Lumenized bloom block render layer
     * @return {@code true} only for a complete, known-empty current render-list state
     */
    boolean isDefinitelyEmpty(BlockRenderLayer bloomLayer);
}
