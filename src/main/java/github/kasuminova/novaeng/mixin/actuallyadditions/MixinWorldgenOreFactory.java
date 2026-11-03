package github.kasuminova.novaeng.mixin.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.gen.AAWorldGen;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;

@Mixin(value = AAWorldGen.class, remap = false)
public abstract class MixinWorldgenOreFactory {
    /**
     * @author NovaEngineering
     * @reason Reuse one immutable vein generator per batch without changing random calls or generation order.
     */
    @Overwrite
    public void addOreSpawn(
        final IBlockState state,
        final Block host,
        final World world,
        final Random random,
        final int blockX,
        final int blockZ,
        final int veinSize,
        final int attempts,
        final int minY,
        final int maxY) {
        if (attempts <= 0) return;
        final WorldGenMinable generator =
            new WorldGenMinable(state, veinSize, BlockMatcher.forBlock(host));
        for (int i = 0; i < attempts; i++) {
            final int x = blockX + random.nextInt(16);
            final int y = minY + random.nextInt(maxY - minY);
            final int z = blockZ + random.nextInt(16);
            generator.generate(world, random, new BlockPos(x, y, z));
        }
    }
}
