package github.kasuminova.novaeng.common.performance;

import journeymap.client.model.chunk.ChunkMD;
import net.minecraft.util.math.BlockPos;

/**
 * Reuses immutable coordinates within repeated chunk scans. A mutable position cannot safely be
 * passed to opaque/modded getBlockState implementations, which may keep their argument.
 */
public final class JourneyMapBlockPositions {
    private static final ThreadLocal<BlockPos[]> POSITIONS = ThreadLocal.withInitial(() -> new BlockPos[65536]);

    private JourneyMapBlockPositions() {
    }

    public static BlockPos fromChunkLocal(final ChunkMD chunk, final int x, final int y, final int z) {
        // Preserve an extension's coordinate semantics. Only the known base implementation is new BlockPos.
        if (chunk.getClass() != ChunkMD.class) return chunk.getBlockPos(x, y, z);
        return atWorld(chunk.toWorldX(x), y, chunk.toWorldZ(z));
    }

    public static BlockPos atWorld(final int x, final int y, final int z) {
        final BlockPos[] cache = POSITIONS.get();
        // All 256 columns/heights of the current chunk fit without collisions. Other chunks,
        // neighbors and extended heights can evict entries, but full coordinate checks prevent aliasing.
        final int index = (x & 15) | ((z & 15) << 4) | ((y & 255) << 8);
        final BlockPos position = cache[index];
        if (position != null && position.getX() == x && position.getY() == y && position.getZ() == z) return position;
        final BlockPos replacement = new BlockPos(x, y, z);
        cache[index] = replacement;
        return replacement;
    }
}
