package github.kasuminova.novaeng.mixin.taiga;

import com.sosnitzka.taiga.util.Generator;
import github.kasuminova.novaeng.common.performance.WorldgenOrePlacement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Random;

@Mixin(value = Generator.class, remap = false)
public abstract class MixinDescendingOre {
    /**
     * @author NovaEngineering
     * @reason Reuse adjacent read results while retaining random draws, placement order and the selected notification policy.
     */
    @Overwrite
    public static void generateOreDescending(final List<IBlockState> replaceBlocks, final IBlockState replacement,
                                             final Random random, final int chunkX, final int chunkZ,
                                             final World world, final int count, final int minY, final int maxY) {
        for (int i = 0; i < count; i++) {
            final int x = chunkX + random.nextInt(16);
            final int z = chunkZ + random.nextInt(16);
            BlockPos pos = new BlockPos(x, maxY, z);
            if (replaceBlocks.contains(world.getBlockState(pos))) {
                if (replaceBlocks.contains(world.getBlockState(pos.up()))) continue;
                WorldgenOrePlacement.setDefault(world, pos, replacement);
            }
            BlockPos below = pos.down();
            boolean matches = replaceBlocks.contains(world.getBlockState(below));
            while (!matches && pos.getY() > minY) {
                pos = below;
                below = pos.down();
                matches = replaceBlocks.contains(world.getBlockState(below));
            }
            if (matches) WorldgenOrePlacement.setDefault(world, below, replacement);
        }
    }
}
