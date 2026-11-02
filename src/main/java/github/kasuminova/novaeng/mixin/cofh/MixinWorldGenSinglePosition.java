package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.generator.WorldGen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Random;

@Mixin(value = WorldGen.class, remap = false)
public abstract class MixinWorldGenSinglePosition {

    /**
     * @author circulation
     * @reason Use one immutable coordinate for a placement's material check and block update without pooling escaping positions.
     */
    @Overwrite
    public static boolean generateBlock(final World world, final Random random, final int x, final int y, final int z,
                                        final WeightedBlock[] materials, final List<WeightedBlock> ores) {
        final BlockPos position = new BlockPos(x, y, z);
        if (materials != null && materials.length != 0 && !WorldGen.canGenerateInBlock(world, position, materials)) {
            return false;
        }
        return WorldGen.setBlock(world, position, WorldGen.selectBlock(random, ores));
    }
}
