package github.kasuminova.novaeng.mixin.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.blocks.BlockMisc;
import de.ellpeck.actuallyadditions.mod.blocks.InitBlocks;
import de.ellpeck.actuallyadditions.mod.blocks.metalists.TheMiscBlocks;
import de.ellpeck.actuallyadditions.mod.config.values.ConfigBoolValues;
import de.ellpeck.actuallyadditions.mod.config.values.ConfigIntValues;
import de.ellpeck.actuallyadditions.mod.gen.AAWorldGen;
import de.ellpeck.actuallyadditions.mod.gen.WorldGenLushCaves;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(value = AAWorldGen.class, remap = false)
public abstract class MixinLushCaveAllocation {
    @Shadow
    @Final
    private WorldGenLushCaves caveGen;

    @Shadow
    public abstract void addOreSpawn(IBlockState state, Block host, World world, Random random, int x, int z, int size, int attempts, int min, int max);

    /**
     * @author NovaEngineering
     * @reason Construct the cave bounding box only after the unchanged probability draw succeeds.
     */
    @Overwrite
    private void generateDefault(World world, Random random, int x, int z) {
        if (ConfigBoolValues.GENERATE_QUARTZ.isEnabled()) {
            this.addOreSpawn(InitBlocks.blockMisc.getDefaultState().withProperty((IProperty) BlockMisc.TYPE, (Comparable) TheMiscBlocks.ORE_QUARTZ), Blocks.STONE, world, random, x * 16, z * 16, MathHelper.getInt(random, 5, 8), 10, 0, 45);
        }
        if (ConfigBoolValues.GEN_LUSH_CAVES.isEnabled()) {
            int randConst = -1768102243;
            Random chunkRand = new Random((long) randConst ^ world.getSeed() ^ (long) (x * 29L + z * 31L));
            if (chunkRand.nextInt(ConfigIntValues.LUSH_CAVE_CHANCE.getValue()) <= 0) {
                StructureBoundingBox box = new StructureBoundingBox(x * 16 + 8, 0, z * 16 + 8, x * 16 + 8 + 15, 255, z * 16 + 8 + 15);
                BlockPos randPos = world.getTopSolidOrLiquidBlock(new BlockPos(x * 16 + MathHelper.getInt(random, 6, 10), 0, z * 16 + MathHelper.getInt(random, 6, 10)));
                BlockPos pos = randPos.down(MathHelper.getInt(chunkRand, 15, randPos.getY() - 15));
                this.caveGen.generate(world, chunkRand, pos, box);
            }
        }
    }
}
