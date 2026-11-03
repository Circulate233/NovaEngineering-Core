package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.state.IBlockState;

/**
 * Access to an exclusively owned section while it is being loaded, not a live-world compactor.
 */
public interface SectionStorageAccess {
    /** Discard cached IDs before the client replaces the contents of an existing palette. */
    void nova$invalidatePaletteSetCache();

    void nova$initializeSection(int bits);

    int nova$sectionId(IBlockState state);

    void nova$writeSectionId(int index, int paletteId);

    /**
     * Populate a newly constructed section, before any world access or publication.
     */
    void nova$importLocalIds(short[] ids, int offset, IBlockState[] states, int stateCount, int[] translatedIds);

    /**
     * Fill a newly constructed section with one non-air state.
     */
    void nova$fillSection(IBlockState state);

    IBlockState nova$readSectionState(int index);

    int nova$sectionBits();

    int nova$readSectionId(int index);

    IBlockState nova$stateForPaletteId(int id);
}
