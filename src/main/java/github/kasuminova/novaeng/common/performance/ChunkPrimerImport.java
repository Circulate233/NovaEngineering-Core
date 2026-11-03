package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.mixin.minecraft.AccessorSectionCounts;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.Arrays;

/**
 * Builds exclusively owned sections before Chunk's normal constructor tail callbacks.
 */
public final class ChunkPrimerImport {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private ChunkPrimerImport() {
    }

    public static boolean tryImport(final Chunk chunk, final ChunkPrimer primer, final boolean skyLight) {
        if (chunk.getClass() != Chunk.class || primer.getClass() != ChunkPrimer.class) return false;
        final ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (final ExtendedBlockStorage section : sections) if (section != Chunk.NULL_BLOCK_STORAGE) return false;
        Scratch scratch = SCRATCH.get();
        if (scratch.inUse) scratch = new Scratch();
        scratch.inUse = true;
        try {
            final IBlockState air = Blocks.AIR.getDefaultState();
            if (air.getClass() != BlockStateContainer.StateImplementation.class
                || !PrimerImportMethods.supports(air.getBlock()) || air.getMaterial() != Material.AIR) return false;
            for (final Section section : scratch.sections) section.begin(air);
            Arrays.fill(scratch.ids, (short) 0);
            // Keep the original x/z/y getter order. Nothing is committed until every state is supported.
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        final IBlockState state = primer.getBlockState(x, y, z);
                        if (state == air) continue;
                        final Section section = scratch.sections[y >> 4];
                        if (!section.accept(state)) return false;
                        scratch.ids[(y >> 4 << 12) | ((y & 15) << 8) | (z << 4) | x] = (short) section.lastId;
                    }
                }
            }
            for (int y = 0; y < 16; y++) {
                final Section section = scratch.sections[y];
                if (section.blockCount == 0) continue;
                final ExtendedBlockStorage storage = new ExtendedBlockStorage(y << 4, skyLight);
                final SectionStorageAccess data = (SectionStorageAccess) storage.getData();
                if (section.blockCount == 4096 && section.states.size() == 2) {
                    data.nova$fillSection(section.states.get(1));
                } else {
                    data.nova$importLocalIds(scratch.ids, y << 12,
                        section.states.elements(), section.states.size(), scratch.paletteIds);
                }
                final AccessorSectionCounts counts = (AccessorSectionCounts) storage;
                counts.nova$setBlockRefCount(section.blockCount);
                counts.nova$setTickRefCount(section.tickCount);
                sections[y] = storage;
            }
            return true;
        } finally {
            for (final Section section : scratch.sections) section.clear();
            scratch.inUse = false;
        }
    }

    private static final class Scratch {
        private final short[] ids = new short[16 * 4096];
        private final int[] paletteIds = new int[4097];
        private final Section[] sections = new Section[16];
        private boolean inUse;

        private Scratch() {
            for (int i = 0; i < sections.length; i++) sections[i] = new Section();
        }
    }

    private static final class Section {
        private final Reference2IntOpenHashMap<IBlockState> stateIds = new Reference2IntOpenHashMap<>(16);
        private final ObjectArrayList<IBlockState> states = ObjectArrayList.wrap(new IBlockState[16], 0);
        private final BooleanArrayList tickable = new BooleanArrayList(16);
        private IBlockState lastState;
        private int lastId;
        private boolean lastTicks;
        private int blockCount;
        private int tickCount;

        private Section() {
            stateIds.defaultReturnValue(-1);
        }

        private void begin(final IBlockState air) {
            stateIds.put(air, 0);
            states.add(air);
            tickable.add(false);
        }

        private boolean accept(final IBlockState state) {
            if (state != lastState) {
                int id = stateIds.getInt(state);
                if (id < 0) {
                    if (state.getClass() != BlockStateContainer.StateImplementation.class) return false;
                    final Block block = state.getBlock();
                    if (!PrimerImportMethods.supports(block)) return false;
                    if (state.getMaterial() == Material.AIR) {
                        id = 0;
                    } else {
                        id = states.size();
                        states.add(state);
                        tickable.add(block.getTickRandomly());
                    }
                    stateIds.put(state, id);
                }
                lastId = id;
                lastTicks = tickable.getBoolean(id);
                lastState = state;
            }
            if (lastId != 0) {
                blockCount++;
                if (lastTicks) tickCount++;
            }
            return true;
        }

        private void clear() {
            stateIds.clear();
            states.clear();
            tickable.clear();
            lastState = null;
            lastId = blockCount = tickCount = 0;
            lastTicks = false;
        }
    }
}
