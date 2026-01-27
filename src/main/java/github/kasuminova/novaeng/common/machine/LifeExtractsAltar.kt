package github.kasuminova.novaeng.common.machine

import crafttweaker.CraftTweakerAPI.itemUtils
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent
import github.kasuminova.mmce.common.event.recipe.RecipeEvent
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.machine.MMAltar.addBlood
import github.kasuminova.novaeng.common.machine.MMAltar.checkRitualWellOfSuffering
import github.kasuminova.novaeng.common.machine.MMAltar.getAltar
import github.kasuminova.novaeng.common.util.Functions
import github.kasuminova.novaeng.common.util.RecipePrimerEx.setLore
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object LifeExtractsAltar : MachineSpecial {

    const val SINGLE_VALUE = 5000
    const val MACHINEID = "life_extracts_altar"
    val REGISTRY_NAME = ResourceLocation(ModularMachinery.MODID, MACHINEID)

    override fun getRegistryName(): ResourceLocation {
        return REGISTRY_NAME
    }

    override fun preInit(machine: DynamicMachine) {
        super.init(machine)
        RecipeBuilder.newBuilder("knzj", MACHINEID, 20)
            .addItemInput(itemUtils.getItem("deepmoblearningbm:digital_agonizer", 0)).setChance(0.0f)
            .addItemInput(itemUtils.getItem("contenttweaker:zbk", 0)).setChance(0.0f)
            .setLore("novaeng.life_extracts_altar.recipe.0")
            .setNBTChecker { ctrl, item ->
                val i = item.internal as ItemStack
                val inbt = i.tagCompound ?: return@setNBTChecker false
                if (inbt.hasKey("binding")) {
                    val nbt = ctrl.controller.customDataTag
                    nbt.setTag("x", inbt.getTag("x"))
                    nbt.setTag("y", inbt.getTag("y"))
                    nbt.setTag("z", inbt.getTag("z"))
                    return@setNBTChecker true
                } else return@setNBTChecker false
            }
            .addPostCheckHandler {
                val mm_altarctrl = getAltarCtrl(it)
                if (mm_altarctrl == null) {
                    it.setFailed("novaeng.life_extracts_altar.failed.0")
                    return@addPostCheckHandler
                } else if (!mm_altarctrl.world.isRemote) {
                    val altar = mm_altarctrl.getAltar()
                    val nbt = mm_altarctrl.customDataTag

                    if (altar == null) {
                        it.setFailed("novaeng.life_extracts_altar.failed.1")
                        return@addPostCheckHandler
                    }

                    if (!checkRitualWellOfSuffering(nbt, mm_altarctrl)) {
                        it.setFailed("novaeng.life_extracts_altar.failed.2")
                    }
                }
            }
            .addStartHandler {
                it.controller.customDataTag.setBoolean("mode", true)
            }
            .addPreTickHandler {
                if (it.activeRecipe.tick != 0) return@addPreTickHandler

                val mm_altarctrl = getAltarCtrl(it)
                if (mm_altarctrl == null) {
                    it.setFailed(true, "novaeng.life_extracts_altar.failed.0")
                    return@addPreTickHandler
                } else if (!mm_altarctrl.world.isRemote) {
                    val altar = mm_altarctrl.getAltar()
                    val nbt = mm_altarctrl.customDataTag

                    if (altar == null) {
                        it.setFailed(true, "novaeng.life_extracts_altar.failed.1")
                        return@addPreTickHandler
                    }

                    if (!checkRitualWellOfSuffering(nbt, mm_altarctrl)) {
                        it.setFailed(true, "novaeng.life_extracts_altar.failed.2")
                    }
                }
            }
            .addFinishHandler {
                getAltarCtrl(it)?.let { mmctrl ->
                    mmctrl.getAltar()
                        .addBlood(SINGLE_VALUE + (SINGLE_VALUE / 10) * mmctrl.customDataTag.getShort("sacrifice"))
                }
            }
            .addRecipeTooltip(
                "novaeng.life_extracts_altar.recipe.1",
                "novaeng.life_extracts_altar.recipe.2",
                "novaeng.life_extracts_altar.recipe.3",
                Functions.getText("novaeng.life_extracts_altar.recipe.4", SINGLE_VALUE)
            )
            .setParallelized(false)
            .build()
        if (NovaEngineeringCore.proxy.isClient) clientInit(machine)
    }

    private fun getAltarCtrl(event: RecipeEvent): TileMultiblockMachineController? {
        val ctrl = event.controller
        val data = ctrl.customDataTag
        val world = ctrl.world
        val mmpos = BlockPos.PooledMutableBlockPos.retain(
            data.getInteger("x"),
            data.getInteger("y"),
            data.getInteger("z")
        )
        val mm_altarctrl =
            world.getTileEntity(mmpos) as? TileMultiblockMachineController
        mmpos.release()
        return mm_altarctrl
    }

    @SideOnly(Side.CLIENT)
    private fun clientInit(machine: DynamicMachine) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent::class.java) {
            val ctrl = it.controller
            val data = ctrl.customDataTag
            val mode = data.getBoolean("mode")

            it.setExtraInfo(
                Functions.getText("gui.life_extracts_altar.tooltip.name"),
                Functions.getText("gui.life_extracts_altar.tooltip.$mode")
            )
        }
    }
}