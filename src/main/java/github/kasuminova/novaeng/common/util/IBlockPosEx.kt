package github.kasuminova.novaeng.common.util

import crafttweaker.annotations.ZenRegister
import crafttweaker.api.world.IBlockPos
import crafttweaker.api.world.IFacing
import crafttweaker.mc1120.world.MCBlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.*
import net.minecraft.util.math.BlockPos
import stanhebben.zenscript.annotations.ZenExpansion
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenExpansion("crafttweaker.world.IBlockPos")
object IBlockPosEx {

    @ZenMethod
    @JvmStatic
    fun IBlockPos.createPosByFacing(facing: IFacing, NorthX: Int, NorthY: Int, NorthZ: Int): IBlockPos {
        return MCBlockPos(
            (this.internal as BlockPos).createPosByFacing(
                (facing.internal as EnumFacing),
                NorthX,
                NorthY,
                NorthZ
            )
        )
    }

    @JvmStatic
    fun BlockPos.createPosByFacing(facing: EnumFacing, x: Int, y: Int, z: Int): BlockPos {
        return when (facing) {
            NORTH -> this.add(x, y, z)
            SOUTH -> this.add(-x, y, -z)
            EAST -> this.add(-z, y, x)
            WEST -> this.add(z, y, -x)
            UP -> this.add(x, -z, y)
            DOWN -> this.add(x, z, y)
        }
    }
}