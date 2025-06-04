package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IFacing;
import crafttweaker.mc1120.world.MCBlockPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("crafttweaker.world.IBlockPos")
public class IBlockPosEx {

    @ZenMethod
    public static IBlockPos createPosByFacing(IBlockPos instance,IFacing facing,int NorthX,int NorthY,int NorthZ){
        return new MCBlockPos(createPosByFacing((BlockPos) instance.getInternal(),(EnumFacing) facing.getInternal(),NorthX,NorthY,NorthZ));
    }

    public static BlockPos createPosByFacing(BlockPos instance, EnumFacing facing, int NorthX, int NorthY, int NorthZ){
        var x = instance.getX() + NorthX;
        var y = instance.getY() + NorthY;
        var z = instance.getZ() + NorthZ;
        return switch (facing) {
            case SOUTH -> new BlockPos(-x,y,-z);
            case EAST -> new BlockPos(-z,y,x);
            case WEST -> new BlockPos(z,y,-x);
            case UP -> new BlockPos(x,-z,y);
            case DOWN -> new BlockPos(x,z,y);
            case NORTH -> new BlockPos(x,y,z);
        };
    }

}
