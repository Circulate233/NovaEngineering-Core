package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Only caches context-independent raw classifications; every query still reads the current block state.
 */
public final class StablePathNodeTypes {
    private static final PathNodeType[] KINDS = PathNodeType.values();
    private static final ClassValue<Integer> METHODS = new ClassValue<>() {
        @Override
        protected Integer computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                if (lookup.revealDirect(lookup.findVirtual(type, "getAiPathNodeType", MethodType.methodType(PathNodeType.class,
                    IBlockState.class, IBlockAccess.class, BlockPos.class, EntityLiving.class))).getDeclaringClass() != Block.class)
                    return 0;
                if (!declaredByBlock(lookup, type, "getMaterial", "func_149688_o", MethodType.methodType(Material.class, IBlockState.class)))
                    return 0;
                return declaredByBlock(lookup, type, "isPassable", "func_176205_b", MethodType.methodType(boolean.class, IBlockAccess.class, BlockPos.class)) ? 2 : 1;
            } catch (final ReflectiveOperationException unavailable) {
                return 0;
            }
        }
    };

    private StablePathNodeTypes() {
    }

    private static boolean declaredByBlock(final MethodHandles.Lookup lookup, final Class<?> type,
                                           final String named, final String srg, final MethodType signature) throws ReflectiveOperationException {
        try {
            return lookup.revealDirect(lookup.findVirtual(type, named, signature)).getDeclaringClass() == Block.class;
        } catch (final NoSuchMethodException obfuscated) {
            return lookup.revealDirect(lookup.findVirtual(type, srg, signature)).getDeclaringClass() == Block.class;
        }
    }

    public static PathNodeType get(final IBlockState state) {
        if (!(state instanceof PathStateMemo memo)) return classify(state);
        final int cached = memo.nova$pathKind();
        if (cached != 0) return cached < 0 ? null : KINDS[cached - 1];
        final PathNodeType result = classify(state);
        memo.nova$pathKind(result == null ? -1 : result.ordinal() + 1);
        return result;
    }

    private static PathNodeType classify(final IBlockState state) {
        if (state.getClass() != BlockStateContainer.StateImplementation.class) return null;
        final Block block = state.getBlock();
        final int methods = METHODS.get(block.getClass());
        if (methods == 0) return null;
        final Material material = state.getMaterial();
        final PathNodeType result;
        if (material == Material.AIR) result = PathNodeType.OPEN;
        else if (block == Blocks.TRAPDOOR || block == Blocks.IRON_TRAPDOOR || block == Blocks.WATERLILY)
            result = PathNodeType.TRAPDOOR;
        else if (block == Blocks.FIRE) result = PathNodeType.DAMAGE_FIRE;
        else if (block == Blocks.CACTUS) result = PathNodeType.DAMAGE_CACTUS;
        else if (block instanceof BlockDoor && state.getValue(BlockDoor.OPEN)) result = PathNodeType.DOOR_OPEN;
        else if (block instanceof BlockDoor && material == Material.WOOD) result = PathNodeType.DOOR_WOOD_CLOSED;
        else if (block instanceof BlockDoor && material == Material.IRON) result = PathNodeType.DOOR_IRON_CLOSED;
        else if (block instanceof BlockRailBase) result = PathNodeType.RAIL;
        else if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate && !state.getValue(BlockFenceGate.OPEN))
            result = PathNodeType.FENCE;
        else if (material == Material.WATER) result = PathNodeType.WATER;
        else if (material == Material.LAVA) result = PathNodeType.LAVA;
        else if (methods == 2) result = material.blocksMovement() ? PathNodeType.BLOCKED : PathNodeType.OPEN;
        else return null;
        return result;
    }
}
