package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.StablePathNodeTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WalkNodeProcessor.class)
public abstract class MixinPathNodeClassification {
    @Shadow(remap = false)
    private EntityLiving currentEntity;

    /**
     * @author NovaEngineering
     * @reason Reuse context-independent state classifications; dynamic hooks still execute with the current world and entity.
     */
    @Overwrite
    protected PathNodeType getPathNodeTypeRaw(IBlockAccess world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        final PathNodeType cached = StablePathNodeTypes.get(state);
        if (cached != null) return cached;
        Block block = state.getBlock();
        Material material = state.getMaterial();

        PathNodeType type = block.getAiPathNodeType(state, world, pos, this.currentEntity);
        if (type != null) return type;

        if (material == Material.AIR) {
            return PathNodeType.OPEN;
        } else if (block == Blocks.TRAPDOOR || block == Blocks.IRON_TRAPDOOR || block == Blocks.WATERLILY) {
            return PathNodeType.TRAPDOOR;
        } else if (block == Blocks.FIRE) {
            return PathNodeType.DAMAGE_FIRE;
        } else if (block == Blocks.CACTUS) {
            return PathNodeType.DAMAGE_CACTUS;
        } else if (block instanceof BlockDoor && material == Material.WOOD && !state.getValue(BlockDoor.OPEN)) {
            return PathNodeType.DOOR_WOOD_CLOSED;
        } else if (block instanceof BlockDoor && material == Material.IRON && !state.getValue(BlockDoor.OPEN)) {
            return PathNodeType.DOOR_IRON_CLOSED;
        } else if (block instanceof BlockDoor && state.getValue(BlockDoor.OPEN)) {
            return PathNodeType.DOOR_OPEN;
        } else if (block instanceof BlockRailBase) {
            return PathNodeType.RAIL;
        } else if (!(block instanceof BlockFence)
            && !(block instanceof BlockWall)
            && (!(block instanceof BlockFenceGate) || state.getValue(BlockFenceGate.OPEN))) {
            if (material == Material.WATER) {
                return PathNodeType.WATER;
            } else if (material == Material.LAVA) {
                return PathNodeType.LAVA;
            } else {
                return block.isPassable(world, pos) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
            }
        } else {
            return PathNodeType.FENCE;
        }
    }
}
