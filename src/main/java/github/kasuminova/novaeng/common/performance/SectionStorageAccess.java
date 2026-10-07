package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.state.IBlockState;

/**
 * Access to an exclusively owned section while it is being loaded, not a live-world compactor.
 */
public interface SectionStorageAccess {
    void nova$initializeSection(int bits);

    void nova$writeSectionState(int index, IBlockState state);

    IBlockState nova$readSectionState(int index);
}
