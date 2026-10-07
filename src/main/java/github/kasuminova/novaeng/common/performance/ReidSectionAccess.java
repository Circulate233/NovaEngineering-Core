package github.kasuminova.novaeng.common.performance;

import net.minecraft.world.chunk.NibbleArray;

/**
 * Temporary serialization fields installed by the supported REID mixin.
 */
public interface ReidSectionAccess {
    int[] nova$getTemporaryPalette();

    void nova$setTemporaryPalette(int[] palette);

    NibbleArray nova$getLegacyAdd2();

    void nova$clearLegacyAdd2();
}
