package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.performance.LeafBlockKinds;
import github.kasuminova.novaeng.common.performance.LeafDecayPropagation;
import github.kasuminova.novaeng.common.performance.LocalChunkReads;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLeaves.class)
public abstract class MixinBlockLeavesPropagation {

    @Shadow
    int[] surroundings;

    @Redirect(method = "updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAreaLoaded(Lnet/minecraft/util/math/BlockPos;I)Z", ordinal = 1),
        require = 1)
    private boolean nova$propagateLoadedLeaves(final World world, final BlockPos center, final int radius) {
        final int centerX = center.getX();
        final int centerY = center.getY();
        final int centerZ = center.getZ();
        if (!world.isAreaLoaded(center, radius)) {
            return false;
        }
        try (final LocalChunkReads reads = LocalChunkReads.borrow(world)) {
            final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
            int supports = 0;
            int onlySupport = -1;
            final int startX = centerX - 4;
            final int startY = centerY - 4;
            final int startZ = centerZ - 4;
            for (int x = 0, indexX = 12 << 10; x < 9; x++, indexX += 1 << 10) {
                final int worldX = startX + x;
                for (int y = 0, indexY = indexX + (12 << 5); y < 9; y++, indexY += 1 << 5) {
                    final int worldY = startY + y;
                    for (int z = 0, index = indexY + 12; z < 9; z++, index++) {
                        position.setPos(worldX, worldY, startZ + z);
                        final IBlockState state = reads.get(position);
                        final Block block = state.getBlock();
                        int kind = NovaEngCoreConfig.SERVER.optimizeWorldgenAuxiliaries ? LeafBlockKinds.of(block) : LeafBlockKinds.DYNAMIC;
                        if (kind == LeafBlockKinds.DYNAMIC) {
                            kind = block.canSustainLeaves(state, world, position) ? 0 : block.isLeaves(state, world, position) ? -2 : -1;
                            reads.invalidate();
                        }
                        this.surroundings[index] = kind;
                        if (kind == 0) {
                            supports++;
                            onlySupport = index;
                        }
                    }
                }
            }
            LeafDecayPropagation.propagate(this.surroundings, supports, onlySupport);
            return false;
        }
    }
}
