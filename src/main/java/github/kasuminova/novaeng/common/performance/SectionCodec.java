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

    public static void decode(final BlockStateContainer container, final byte[] blocks,
                              final NibbleArray data, final NibbleArray add) {
        final Scratch scratch = acquire();
        final ReidSectionAccess reid = (ReidSectionAccess) container;
        final SectionStorageAccess storage = (SectionStorageAccess) container;
        try {
            final NibbleArray add2 = reid.nova$getLegacyAdd2();
            SectionIdCodec.decode(blocks, data.getData(), add == null ? null : add.getData(),
                add2 == null ? null : add2.getData(), reid.nova$getTemporaryPalette(), scratch.ids);
            final IBlockState air = Blocks.AIR.getDefaultState();
            scratch.palette.indexFor(air, 0);
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                IBlockState state = scratch.ids[i] == 0 ? air : Block.BLOCK_STATE_IDS.getByValue(scratch.ids[i]);
                if (state == null) {
                    // BlockStateContainer.get() also interprets an unknown/null palette entry as air.
                    state = air;
                }
                scratch.states[i] = state;
                scratch.palette.indexFor(state, scratch.ids[i]);
            }
            storage.nova$initializeSection(SectionIdCodec.bitsFor(scratch.palette.size()));
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                storage.nova$writeSectionState(i, scratch.states[i]);
            }
            PerformanceMetrics.add(PerformanceMetrics.Counter.SECTION_DECODED, 1);
            PerformanceMetrics.add(PerformanceMetrics.Counter.SECTION_STATES, scratch.palette.size());
        } finally {
            reid.nova$setTemporaryPalette(null);
            reid.nova$clearLegacyAdd2();
            scratch.clear();
        }
    }

    public static NibbleArray encode(final BlockStateContainer container, final byte[] blocks, final NibbleArray data) {
        final Scratch scratch = acquire();
        try {
            final SectionStorageAccess storage = (SectionStorageAccess) container;
            final byte[] nibbles = data.getData();
            for (int i = 0; i < SectionIdCodec.SIZE; i++) {
                final IBlockState state = storage.nova$readSectionState(i);
                int id = scratch.palette.indexOf(state);
                if (id < 0) {
                    id = scratch.palette.indexFor(state, Block.BLOCK_STATE_IDS.get(state));
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
        private final IBlockState[] states = new IBlockState[SectionIdCodec.SIZE];
        private final SectionPalette<IBlockState> palette = new SectionPalette<>();
        private boolean inUse;

        private void clear() {
            Arrays.fill(states, null);
            palette.clear();
            inUse = false;
        }
    }
}
