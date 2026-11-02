package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.base.Predicate;
import github.kasuminova.novaeng.common.performance.VanillaOreGeometryScratch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(WorldGenMinable.class)
public abstract class MixinWorldGenMinableGeometry {

    @Shadow
    public IBlockState oreBlock;

    @Shadow
    public int numberOfBlocks;

    @Shadow
    public Predicate<IBlockState> predicate;

    /**
     * @author circulation
     * @reason Reuse exact Z distances and prune impossible branches while preserving double arithmetic and repeated placements.
     */
    @Overwrite
    public boolean generate(final World world, final Random random, final BlockPos position) {
        final float angle = random.nextFloat() * (float) Math.PI;
        final double xStart = position.getX() + 8 + MathHelper.sin(angle) * this.numberOfBlocks / 8.0F;
        final double xEnd = position.getX() + 8 - MathHelper.sin(angle) * this.numberOfBlocks / 8.0F;
        final double zStart = position.getZ() + 8 + MathHelper.cos(angle) * this.numberOfBlocks / 8.0F;
        final double zEnd = position.getZ() + 8 - MathHelper.cos(angle) * this.numberOfBlocks / 8.0F;
        final double yStart = position.getY() + random.nextInt(3) - 2;
        final double yEnd = position.getY() + random.nextInt(3) - 2;
        if (this.numberOfBlocks <= 0) {
            return true;
        }
        final double[] zDistances = VanillaOreGeometryScratch.borrow();
        try {
            for (int i = 0; i < this.numberOfBlocks; i++) {
                final float progress = (float) i / this.numberOfBlocks;
                final double centerX = xStart + (xEnd - xStart) * progress;
                final double centerY = yStart + (yEnd - yStart) * progress;
                final double centerZ = zStart + (zEnd - zStart) * progress;
                final double size = random.nextDouble() * this.numberOfBlocks / 16.0D;
                final double diameter = (MathHelper.sin((float) Math.PI * progress) + 1.0F) * size + 1.0D;
                final double radius = diameter / 2.0D;
                final int startX = MathHelper.floor(centerX - radius);
                final int startY = MathHelper.floor(centerY - radius);
                final int startZ = MathHelper.floor(centerZ - radius);
                final int stopX = MathHelper.floor(centerX + radius);
                final int stopY = MathHelper.floor(centerY + radius);
                final int stopZ = MathHelper.floor(centerZ + radius);
                final long countZ = (long) stopZ - startZ + 1;
                final boolean table = countZ > 0 && countZ <= zDistances.length;
                double minimumZ = 0.0D;
                if (table) {
                    minimumZ = Double.POSITIVE_INFINITY;
                    for (int offset = 0; offset < countZ; offset++) {
                        double distance = (startZ + offset + 0.5D - centerZ) / radius;
                        distance *= distance;
                        zDistances[offset] = distance;
                        minimumZ = Math.min(minimumZ, distance);
                    }
                    if (!(minimumZ < 1.0D)) {
                        continue;
                    }
                }
                for (int x = startX; x <= stopX; x++) {
                    double distanceX = (x + 0.5D - centerX) / radius;
                    distanceX *= distanceX;
                    if (!(distanceX + minimumZ < 1.0D)) {
                        continue;
                    }
                    for (int y = startY; y <= stopY; y++) {
                        double distanceY = (y + 0.5D - centerY) / radius;
                        distanceY *= distanceY;
                        final double distanceXY = distanceX + distanceY;
                        if (!(distanceXY + minimumZ < 1.0D)) {
                            continue;
                        }
                        for (int z = startZ; z <= stopZ; z++) {
                            double distanceZ;
                            if (table) {
                                distanceZ = zDistances[z - startZ];
                            } else {
                                distanceZ = (z + 0.5D - centerZ) / radius;
                                distanceZ *= distanceZ;
                            }
                            if (distanceXY + distanceZ < 1.0D) {
                                final BlockPos blockPosition = new BlockPos(x, y, z);
                                final IBlockState state = world.getBlockState(blockPosition);
                                if (state.getBlock().isReplaceableOreGen(state, world, blockPosition, this.predicate)) {
                                    world.setBlockState(blockPosition, this.oreBlock, 2);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            VanillaOreGeometryScratch.release(zDistances);
        }
        return true;
    }
}
