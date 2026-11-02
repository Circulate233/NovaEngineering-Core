package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.init.WorldHandler;
import cofh.cofhworld.init.WorldProps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;

@Mixin(value = WorldHandler.class, remap = false)
public abstract class MixinFlatBedrock {
    /**
     * @author NovaEngineering
     * @reason Avoid a discarded biome lookup and reuse the scan probe, retaining immutable placement positions.
     */
    @Overwrite
    public void replaceBedrock(Random random, int chunkX, int chunkZ, World world, boolean newGen) {
        if (!WorldProps.enableFlatBedrock | !newGen & !WorldProps.enableRetroactiveFlatBedrock) {
            return;
        }
        int offsetX = chunkX * 16 + 8;
        int offsetZ = chunkZ * 16 + 8;
        boolean isVoidAge = !world.getBlockState(new BlockPos(offsetX, 0, offsetZ)).getBlock().isAssociatedBlock(Blocks.BEDROCK);
        isVoidAge |= !world.getBlockState(new BlockPos(offsetX + 4, 0, offsetZ + 4)).getBlock().isAssociatedBlock(Blocks.BEDROCK);
        isVoidAge |= !world.getBlockState(new BlockPos(offsetX + 8, 0, offsetZ + 8)).getBlock().isAssociatedBlock(Blocks.BEDROCK);
        if (isVoidAge |= !world.getBlockState(new BlockPos(offsetX + 12, 0, offsetZ + 12)).getBlock().isAssociatedBlock(Blocks.BEDROCK)) {
            return;
        }
        IBlockState filler = switch (world.provider.getDimension()) {
            case -1 -> Blocks.NETHERRACK.getDefaultState();
            case 0 -> Blocks.STONE.getDefaultState();
            case 1 -> Blocks.END_STONE.getDefaultState();
            default -> world.getBiome(new BlockPos(offsetX, 0, offsetZ)).fillerBlock;
        };
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int blockX = 0; blockX < 16; ++blockX) {
            for (int blockZ = 0; blockZ < 16; ++blockZ) {
                IBlockState state;
                BlockPos pos;
                int blockY;
                for (blockY = 5; blockY > WorldProps.numBedrockLayers - 1; --blockY) {
                    pos = probe.setPos(offsetX + blockX, blockY, offsetZ + blockZ);
                    state = world.getBlockState(pos);
                    if (!state.getBlock().isAssociatedBlock(Blocks.BEDROCK)) continue;
                    world.setBlockState(pos.toImmutable(), filler, 2);
                }
                for (blockY = WorldProps.numBedrockLayers - 1; blockY > 0; --blockY) {
                    pos = probe.setPos(offsetX + blockX, blockY, offsetZ + blockZ);
                    state = world.getBlockState(pos);
                    if (state.getBlock().isAssociatedBlock(Blocks.BEDROCK)) continue;
                    world.setBlockState(pos.toImmutable(), Blocks.BEDROCK.getDefaultState(), 2);
                }
            }
        }
        int worldHeight = world.getActualHeight();
        if (world.getBlockState(new BlockPos(offsetX, worldHeight - 1, offsetZ)).getBlock().isAssociatedBlock(Blocks.BEDROCK)) {
            for (int blockX = 0; blockX < 16; ++blockX) {
                for (int blockZ = 0; blockZ < 16; ++blockZ) {
                    IBlockState state;
                    BlockPos pos;
                    int blockY;
                    for (blockY = worldHeight - 2; blockY > worldHeight - 6; --blockY) {
                        pos = probe.setPos(offsetX + blockX, blockY, offsetZ + blockZ);
                        state = world.getBlockState(pos);
                        if (!state.getBlock().isAssociatedBlock(Blocks.BEDROCK)) continue;
                        world.setBlockState(pos.toImmutable(), filler, 2);
                    }
                    for (blockY = worldHeight - WorldProps.numBedrockLayers; blockY < worldHeight - 1; ++blockY) {
                        pos = probe.setPos(offsetX + blockX, blockY, offsetZ + blockZ);
                        state = world.getBlockState(pos);
                        if (state.getBlock().isAssociatedBlock(Blocks.BEDROCK)) continue;
                        world.setBlockState(pos.toImmutable(), Blocks.BEDROCK.getDefaultState(), 2);
                    }
                }
            }
        }
    }
}
