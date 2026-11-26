package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapBlockPositions;
import journeymap.client.model.block.BlockMD;
import journeymap.client.model.chunk.ChunkMD;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockMD.class, remap = false)
public abstract class MixinBlockMDScanPositions {
    @Redirect(method = "getBlockMDFromChunkLocal",
        at = @At(value = "INVOKE", target = "Ljourneymap/client/model/chunk/ChunkMD;getBlockPos(III)Lnet/minecraft/util/math/BlockPos;"),
        remap = false, require = 1)
    private static BlockPos nova$scanPosition(final ChunkMD chunk, final int x, final int y, final int z) {
        return JourneyMapBlockPositions.fromChunkLocal(chunk, x, y, z);
    }
}
