package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.BlockStone;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import rtg.api.world.gen.RTGChunkGenSettings;

public final class VanillaPrimerOreRules {
    private VanillaPrimerOreRules() {
    }

    public static PrimerOreRule[] compile(final ChunkGeneratorSettings s, final World world) {
        return new PrimerOreRule[]{
            PrimerOreGeneration.vanilla(world, "DIRT", Blocks.DIRT.getDefaultState(), s.dirtCount, s.dirtSize, s.dirtMinHeight, s.dirtMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GRAVEL", Blocks.GRAVEL.getDefaultState(), s.gravelCount, s.gravelSize, s.gravelMinHeight, s.gravelMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "DIORITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE), s.dioriteCount, s.dioriteSize, s.dioriteMinHeight, s.dioriteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GRANITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE), s.graniteCount, s.graniteSize, s.graniteMinHeight, s.graniteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "ANDESITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE), s.andesiteCount, s.andesiteSize, s.andesiteMinHeight, s.andesiteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "COAL", Blocks.COAL_ORE.getDefaultState(), s.coalCount, s.coalSize, s.coalMinHeight, s.coalMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "IRON", Blocks.IRON_ORE.getDefaultState(), s.ironCount, s.ironSize, s.ironMinHeight, s.ironMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GOLD", Blocks.GOLD_ORE.getDefaultState(), s.goldCount, s.goldSize, s.goldMinHeight, s.goldMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "REDSTONE", Blocks.REDSTONE_ORE.getDefaultState(), s.redstoneCount, s.redstoneSize, s.redstoneMinHeight, s.redstoneMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "DIAMOND", Blocks.DIAMOND_ORE.getDefaultState(), s.diamondCount, s.diamondSize, s.diamondMinHeight, s.diamondMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "LAPIS", Blocks.LAPIS_ORE.getDefaultState(), s.lapisCount, s.lapisSize, s.lapisCenterHeight, s.lapisSpread, true)
        };
    }

    public static PrimerOreRule[] compile(final RTGChunkGenSettings s, final World world) {
        return new PrimerOreRule[]{
            PrimerOreGeneration.vanilla(world, "DIRT", Blocks.DIRT.getDefaultState(), s.dirtCount, s.dirtSize, s.dirtMinHeight, s.dirtMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GRAVEL", Blocks.GRAVEL.getDefaultState(), s.gravelCount, s.gravelSize, s.gravelMinHeight, s.gravelMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "DIORITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE), s.dioriteCount, s.dioriteSize, s.dioriteMinHeight, s.dioriteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GRANITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE), s.graniteCount, s.graniteSize, s.graniteMinHeight, s.graniteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "ANDESITE", Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE), s.andesiteCount, s.andesiteSize, s.andesiteMinHeight, s.andesiteMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "COAL", Blocks.COAL_ORE.getDefaultState(), s.coalCount, s.coalSize, s.coalMinHeight, s.coalMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "IRON", Blocks.IRON_ORE.getDefaultState(), s.ironCount, s.ironSize, s.ironMinHeight, s.ironMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "GOLD", Blocks.GOLD_ORE.getDefaultState(), s.goldCount, s.goldSize, s.goldMinHeight, s.goldMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "REDSTONE", Blocks.REDSTONE_ORE.getDefaultState(), s.redstoneCount, s.redstoneSize, s.redstoneMinHeight, s.redstoneMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "DIAMOND", Blocks.DIAMOND_ORE.getDefaultState(), s.diamondCount, s.diamondSize, s.diamondMinHeight, s.diamondMaxHeight, false),
            PrimerOreGeneration.vanilla(world, "LAPIS", Blocks.LAPIS_ORE.getDefaultState(), s.lapisCount, s.lapisSize, s.lapisCenterHeight, s.lapisSpread, true)
        };
    }
}
