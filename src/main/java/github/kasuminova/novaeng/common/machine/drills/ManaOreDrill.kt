package github.kasuminova.novaeng.common.machine.drills

import crafttweaker.api.item.IIngredient
import crafttweaker.api.minecraft.CraftTweakerMC
import net.minecraftforge.fluids.FluidRegistry

object ManaOreDrill : Drill() {
    override fun getCoreTheardName(): String {
        return "novaeng.drill.thread.c"
    }

    override fun getMachineName(): String {
        return "mana_ore_drill"
    }

    override fun getType(): Type {
        return Type.SINGLE
    }

    override fun getBaseEnergy(): Long {
        return 24576
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
