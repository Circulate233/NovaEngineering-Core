package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.NibbleArray;

import java.util.Arrays;

/**
 * Bulk section IO at the Anvil boundary; networking and ordinary palette growth stay vanilla.
 */
public final class SectionCodec {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private SectionCodec() {
    }

    public static void decode(
        final BlockStateContainer container,
        final byte[] blocks,
        final NibbleArray data,
        final NibbleArray add) {
        final Scratch scratch = acquire();
        final ReidSectionAccess reid = (ReidSectionAccess) container;
        final SectionStorageAccess storage = (SectionStorageAccess) container;
        try {
            final NibbleArray add2 = reid.nova$getLegacyAdd2();
            SectionIdCodec.decode(
                blocks,
                data.getData(),
                add == null ? null : add.getData(),
                add2 == null ? null : add2.getData(),
                reid.nova$getTemporaryPalette(),
                scratch.ids);
            final IBlockState air = Blocks.AIR.getDefaultState();
            scratch.palette.indexFor(air, 0);
            scratch.states[0] = air;
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                IBlockState state =
                    scratch.ids[i] == 0
                        ? air
                        : Block.BLOCK_STATE_IDS.getByValue(scratch.ids[i]);
                if (state == null) {
                    // BlockStateContainer.get() also interprets an unknown/null palette entry as
                    // air.
                    state = air;
                }
                final int id = scratch.palette.indexFor(state, scratch.ids[i]);
                scratch.states[id] = state;
                scratch.ids[i] = id;
            }
            final int paletteSize = scratch.palette.size();
            storage.nova$initializeSection(SectionIdCodec.bitsFor(paletteSize));
            // Local insertion ids and global registry ids differ; ask the destination palette once
            // per state.
            for (int i = 0; i < paletteSize; i++) {
                scratch.paletteIds[i] = storage.nova$sectionId(scratch.states[i]);
            }
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                storage.nova$writeSectionId(i, scratch.paletteIds[scratch.ids[i]]);
            }
            PerformanceMetrics.add(PerformanceMetrics.Counter.SECTION_DECODED, 1);
            PerformanceMetrics.add(
                PerformanceMetrics.Counter.SECTION_STATES, scratch.palette.size());
        } finally {
            reid.nova$setTemporaryPalette(null);
            reid.nova$clearLegacyAdd2();
            scratch.clear();
        }
    }

    public static NibbleArray encode(
        final BlockStateContainer container, final byte[] blocks, final NibbleArray data) {
        final Scratch scratch = acquire();
        try {
            final SectionStorageAccess storage = (SectionStorageAccess) container;
            final byte[] nibbles = data.getData();
            final int bits = storage.nova$sectionBits();
            // Local palette IDs are stable for this synchronous section snapshot. Resolve each
            // source ID once; global palettes may be too wide for the bounded scratch array.
            final boolean localPalette = bits >= 0 && bits <= 12;
            if (localPalette) Arrays.fill(scratch.paletteIds, 0, 1 << bits, 0);
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                final int id;
                if (localPalette) {
                    final int sourceId = storage.nova$readSectionId(i);
                    int translated = scratch.paletteIds[sourceId];
                    if (translated == 0) {
                        final IBlockState state = storage.nova$stateForPaletteId(sourceId);
                        int index = scratch.palette.indexOf(state);
                        if (index < 0)
                            index = scratch.palette.addNew(state, Block.BLOCK_STATE_IDS.get(state));
                        scratch.paletteIds[sourceId] = translated = index + 1;
                    }
                    id = translated - 1;
                } else {
                    final IBlockState state = storage.nova$readSectionState(i);
                    int index = scratch.palette.indexOf(state);
                    if (index < 0)
                        index = scratch.palette.addNew(state, Block.BLOCK_STATE_IDS.get(state));
                    id = index;
                }
                SectionIdCodec.writeIndex(i, id, blocks, nibbles);
            }
            // REID's existing writePaletteNBT hook transfers this owning array to the chunk's NBT.
            ((ReidSectionAccess) container).nova$setTemporaryPalette(scratch.palette.snapshot());
            PerformanceMetrics.add(PerformanceMetrics.Counter.SECTION_ENCODED, 1);
            return null;
        } finally {
            scratch.clear();
        }
    }

    private static Scratch acquire() {
        Scratch scratch = SCRATCH.get();
        if (scratch.inUse) {
            scratch = new Scratch();
        }
        scratch.inUse = true;
        return scratch;
    }

    private static final class Scratch {
        private final int[] ids = new int[SectionIdCodec.SIZE];
        private final IBlockState[] states = new IBlockState[SectionIdCodec.SIZE + 1];
        private final int[] paletteIds = new int[SectionIdCodec.SIZE + 1];
        private final SectionPalette<IBlockState> palette = new SectionPalette<>();
        private boolean inUse;

        private void clear() {
            Arrays.fill(states, 0, palette.size(), null);
            palette.clear();
            inUse = false;
        }
    }
}
