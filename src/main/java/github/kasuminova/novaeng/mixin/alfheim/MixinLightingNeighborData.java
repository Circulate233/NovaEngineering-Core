package github.kasuminova.novaeng.mixin.alfheim;

import dev.redstudio.alfheim.lighting.LightingEngine;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LightingEngine.class, remap = false)
public abstract class MixinLightingNeighborData {
    @Shadow
    @Final
    private static long[] neighborShifts;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos currentPos;
    @Shadow
    @Final
    private LightingEngine.NeighborInfo[] neighborInfos;
    @Shadow
    private Chunk currentChunk;
    @Shadow
    private long currentChunkIdentifier;
    @Shadow
    private long currentData;
    @Shadow
    private boolean isNeighborDataValid;

    @Shadow
    private static BlockPos.MutableBlockPos decodeWorldCoord(final BlockPos.MutableBlockPos pos, final long data) {
        throw new AssertionError();
    }

    @Shadow
    private static byte getCachedLightFor(final Chunk chunk, final ExtendedBlockStorage section,
                                          final BlockPos pos, final EnumSkyBlock type) {
        throw new AssertionError();
    }

    @Shadow
    private Chunk getChunk(final BlockPos pos) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Reuse decoded coordinates and the current section for neighboring light probes.
     */
    @Overwrite
    private void fetchNeighborDataFromCursor(final EnumSkyBlock type) {
        if (isNeighborDataValid) return;
        isNeighborDataValid = true;
        final int x = currentPos.getX();
        final int y = currentPos.getY();
        final int z = currentPos.getZ();
        final boolean standardChunk = currentChunk != null && currentChunk.getClass() == Chunk.class;
        final ExtendedBlockStorage[] sections = standardChunk ? currentChunk.getBlockStorageArray() : null;
        final ExtendedBlockStorage currentSection = standardChunk ? sections[y >> 4] : null;
        for (int i = 0; i < neighborInfos.length; i++) {
            final LightingEngine.NeighborInfo neighbor = neighborInfos[i];
            final long key = neighbor.key = currentData + neighborShifts[i];
            if ((key & 0x1000000000000000L) != 0) {
                neighbor.chunk = null;
                continue;
            }
            final BlockPos.MutableBlockPos pos = neighbor.mutableBlockPos;
            final Chunk chunk;
            final ExtendedBlockStorage section;
            if (standardChunk && (key & 0xFFFFFC3FFFFF0L) == currentChunkIdentifier) {
                final EnumFacing direction = EnumFacing.VALUES[i];
                // Out-of-world cursors wrap when decoded; their valid neighbors must use the packed height.
                final int neighborY = (int) (key >>> 52 & 0xFFL);
                pos.setPos(x + direction.getXOffset(), neighborY, z + direction.getZOffset());
                chunk = neighbor.chunk = currentChunk;
                section = (neighborY >> 4) == (y >> 4) ? currentSection : sections[neighborY >> 4];
            } else {
                decodeWorldCoord(pos, key);
                chunk = neighbor.chunk = (key & 0xFFFFFC3FFFFF0L) == currentChunkIdentifier
                    ? currentChunk : getChunk(pos);
                if (chunk == null) continue;
                section = chunk.getBlockStorageArray()[pos.getY() >> 4];
            }
            neighbor.light = getCachedLightFor(chunk, section, pos, type);
        }
    }
}
