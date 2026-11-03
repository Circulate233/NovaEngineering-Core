package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngCoreConfig;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;

import java.util.Arrays;

/**
 * An exclusively borrowed read window. Call invalidate around every world-mutating or unknown callback.
 */
public final class LocalChunkReads implements AutoCloseable {
    private static final ThreadLocal<LocalChunkReads> IDLE = ThreadLocal.withInitial(LocalChunkReads::new);
    private final long[] keys = new long[4];
    private final Chunk[] chunks = new Chunk[4];
    private World world;
    private boolean active;
    private boolean supported;
    private int size;

    private LocalChunkReads() {
    }

    public static LocalChunkReads borrow(final World world) {
        LocalChunkReads reads = IDLE.get();
        if (reads.active) reads = new LocalChunkReads();
        reads.active = true;
        reads.world = world;
        reads.supported = NovaEngCoreConfig.SERVER.optimizeWorldgenAuxiliaries
            && (world.getClass() == WorldServer.class || world.getClass() == WorldServerMulti.class)
            && world.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES;
        return reads;
    }

    public IBlockState get(final BlockPos pos) {
        if (!supported || world.isOutsideBuildHeight(pos)) return world.getBlockState(pos);
        final long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                final Chunk chunk = chunks[i];
                chunk.unloadQueued = false;
                return chunk.getBlockState(pos);
            }
        }
        final Chunk chunk = world.getChunk(pos);
        if (chunk.getClass() != Chunk.class) return chunk.getBlockState(pos);
        if (size == chunks.length) invalidate();
        keys[size] = key;
        chunks[size++] = chunk;
        return chunk.getBlockState(pos);
    }

    public void invalidate() {
        Arrays.fill(chunks, 0, size, null);
        size = 0;
    }

    public void disable() {
        invalidate();
        supported = false;
    }

    @Override
    public void close() {
        invalidate();
        world = null;
        supported = active = false;
    }
}
