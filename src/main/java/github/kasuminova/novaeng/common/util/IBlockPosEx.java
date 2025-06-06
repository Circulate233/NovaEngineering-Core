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

    public static BlockPos createPosByFacing(BlockPos instance, EnumFacing facing, int x, int y, int z){
        return switch (facing) {
            case NORTH -> instance.add(x,y,z);
            case SOUTH -> instance.add(-x,y,-z);
            case EAST -> instance.add(-z,y,x);
            case WEST -> instance.add(z,y,-x);
            case UP -> instance.add(x,-z,y);
            case DOWN -> instance.add(x,z,y);
        };
    }
}
