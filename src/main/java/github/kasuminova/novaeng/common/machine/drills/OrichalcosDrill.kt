package github.kasuminova.novaeng.common.machine.drills

import crafttweaker.api.item.IIngredient
import crafttweaker.api.minecraft.CraftTweakerMC
import net.minecraftforge.fluids.FluidRegistry

object OrichalcosDrill : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.c"
    }

    override fun getMachineName(): String {
        return "orichalcos_drill"
    }

    override fun getType(): Type {
        return Type.RANGE
    }

    override fun getAdvancedRecipeTimeMultiple(): Float {
        return 1.5f
    }

    override fun getBaseEnergy(): Long {
        return 32768
    }

    override fun isDimensional(): Boolean {
        return true
    }

    private val i = arrayOf<IIngredient>(
        CraftTweakerMC.getILiquidStack(FluidRegistry.getFluidStack("fluidedmana", 1))
    )

    override fun getExIngredient(): Array<IIngredient> {
        return i
    }
}