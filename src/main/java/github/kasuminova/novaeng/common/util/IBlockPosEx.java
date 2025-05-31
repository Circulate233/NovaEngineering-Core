package github.kasuminova.novaeng.common.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IFacing;
import crafttweaker.mc1120.world.MCBlockPos;
import net.minecraft.util.EnumFacing;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenExpansion("crafttweaker.world.IBlockPos")
public class IBlockPosEx {

    @ZenMethod
    public static IBlockPos createPosByFacing(IBlockPos instance,IFacing facing,int NorthX,int NorthY,int NorthZ){
        var x = instance.getX();
        var y = instance.getY();
        var z = instance.getZ();
        return switch ((EnumFacing) facing.getInternal()) {
            case SOUTH -> new MCBlockPos(-x,y,-z);
            case EAST -> new MCBlockPos(-z,y,x);
            case WEST -> new MCBlockPos(z,y,-x);
            case UP -> new MCBlockPos(x,-z,y);
            case DOWN -> new MCBlockPos(x,z,y);
            default -> instance;
        };
    }

}
