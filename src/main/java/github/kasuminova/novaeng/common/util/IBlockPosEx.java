package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IFacing;
import crafttweaker.mc1120.world.MCBlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("crafttweaker.world.IBlockPos")
public final class IBlockPosEx {

    private IBlockPosEx() {
    }

    @ZenMethod
    public static IBlockPos createPosByFacing(final IBlockPos position, final IFacing facing,
                                              final int x, final int y, final int z) {
        return new MCBlockPos(createPosByFacing((BlockPos) position.getInternal(),
            (EnumFacing) facing.getInternal(), x, y, z));
    }

    public static BlockPos createPosByFacing(final BlockPos position, final EnumFacing facing,
                                             final int x, final int y, final int z) {
        return switch (facing) {
            case NORTH -> position.add(x, y, z);
            case SOUTH -> position.add(-x, y, -z);
            case EAST -> position.add(-z, y, x);
            case WEST -> position.add(z, y, -x);
            case UP -> position.add(x, -z, y);
            case DOWN -> position.add(x, z, y);
        };
    }
}
