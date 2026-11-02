package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.data.numbers.INumberProvider;
import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.generator.WorldGen;
import cofh.cofhworld.world.generator.WorldGenMinableCluster;
import github.kasuminova.novaeng.common.performance.OreGeometryScratch;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Random;

@Mixin(value = WorldGenMinableCluster.class, remap = false)
public abstract class MixinWorldGenMinableClusterGeometry {

    @Shadow
    @Final
    private List<WeightedBlock> cluster;

    @Shadow
    @Final
    private INumberProvider genClusterSize;

    @Shadow
    @Final
    private WeightedBlock[] genBlock;

    @Shadow
    public abstract boolean generateTiny(World world, Random random, int clusterSize, int x, int y, int z);

    /**
     * @author circulation
     * @reason Reuse each ellipsoid's Z distances and their exact minimum to prune impossible branches without changing visits or RNG calls.
     */
    @Overwrite
    public boolean generate(final World world, final Random random, final BlockPos position) {
        final int x = position.getX();
        final int y = position.getY();
        final int z = position.getZ();
        final int blocks = MathHelper.clamp(this.genClusterSize.intValue(world, random,
            new INumberProvider.DataHolder(position)), 1, 42);
        if (blocks < 4) {
            return this.generateTiny(world, random, blocks, x, y, z);
        }
        final float angle = random.nextFloat() * (float) Math.PI;
        final float xMin = (float) x + MathHelper.sin(angle) * blocks / 8.0F;
        float xDelta = (float) x - MathHelper.sin(angle) * blocks / 8.0F;
        final float zMin = (float) z + MathHelper.cos(angle) * blocks / 8.0F;
        float zDelta = (float) z - MathHelper.cos(angle) * blocks / 8.0F;
        final float yMin = y + random.nextInt(3) - 2;
        float yDelta = y + random.nextInt(3) - 2;
        xDelta -= xMin;
        yDelta -= yMin;
        zDelta -= zMin;
        boolean generated = false;
        final float[] zDistances = OreGeometryScratch.borrow();
        try {
            for (int i = 0; i <= blocks; i++) {
                final float centerX = xMin + xDelta * i / blocks;
                final float centerY = yMin + yDelta * i / blocks;
                final float centerZ = zMin + zDelta * i / blocks;
                final float size = (float) random.nextDouble() * blocks / 16.0F;
                final float radius = ((MathHelper.sin((float) i * (float) Math.PI / blocks) + 1.0F) * size + 1.0F) * 0.5F;
                final int startX = MathHelper.floor(centerX - radius);
                final int startY = MathHelper.floor(centerY - radius);
                final int startZ = MathHelper.floor(centerZ - radius);
                final int stopX = MathHelper.floor(centerX + radius);
                final int stopY = MathHelper.floor(centerY + radius);
                final int stopZ = MathHelper.floor(centerZ + radius);
                final long zCount = (long) stopZ - startZ + 1;
                final boolean table = zCount > 0 && zCount <= zDistances.length;
                // Far-coordinate float rounding can make a geometrically nearest block center non-minimal.
                float nearestZDistance = 0.0F;
                if (table) {
                    nearestZDistance = Float.POSITIVE_INFINITY;
                    for (int offset = 0; offset < zCount; offset++) {
                        float distance = ((float) (startZ + offset) + 0.5F - centerZ) / radius;
                        distance *= distance;
                        zDistances[offset] = distance;
                        nearestZDistance = Math.min(nearestZDistance, distance);
                    }
                    if (nearestZDistance >= 1.0F) {
                        continue;
                    }
                }
                for (int blockX = startX; blockX <= stopX; blockX++) {
                    float xDistance = ((float) blockX + 0.5F - centerX) / radius;
                    xDistance *= xDistance;
                    if (nearestZDistance + xDistance >= 1.0F) {
                        continue;
                    }
                    for (int blockY = startY; blockY <= stopY; blockY++) {
                        float yDistance = ((float) blockY + 0.5F - centerY) / radius;
                        yDistance *= yDistance;
                        final float xyDistance = yDistance + xDistance;
                        if (nearestZDistance + xyDistance >= 1.0F) {
                            continue;
                        }
                        for (int blockZ = startZ; blockZ <= stopZ; blockZ++) {
                            float zDistance;
                            if (table) {
                                zDistance = zDistances[blockZ - startZ];
                            } else {
                                zDistance = ((float) blockZ + 0.5F - centerZ) / radius;
                                zDistance *= zDistance;
                            }
                            if (!(zDistance + xyDistance >= 1.0F)) {
                                generated |= WorldGen.generateBlock(world, random, blockX, blockY, blockZ,
                                    this.genBlock, this.cluster);
                            }
                        }
                    }
                }
            }
        } finally {
            OreGeometryScratch.release(zDistances);
        }
        return generated;
    }
}
