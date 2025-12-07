package github.kasuminova.novaeng.common.machine

import crafttweaker.CraftTweakerAPI
import crafttweaker.api.item.IItemStack
import crafttweaker.api.minecraft.CraftTweakerMC
import crafttweaker.api.oredict.IOreDictEntry
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent
import github.kasuminova.mmce.common.helper.IMachineController
import github.kasuminova.novaeng.common.handler.OreHandler
import github.kasuminova.novaeng.common.util.RecipePrimerEx.setLore
import github.kasuminova.novaeng.common.util.StringUtils
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IngredientArrayBuilder
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import morph.avaritia.recipe.AvaritiaRecipeManager
import net.minecraft.util.ResourceLocation
import kotlin.math.floor
import kotlin.math.roundToLong

object SpaceGenerator : MachineSpecial {

    private const val MACHINEID = "space_generator"
    val REGISTRY_NAME: ResourceLocation = ResourceLocation(ModularMachinery.MODID, MACHINEID)
    private const val ORE_OD = "ore"
    private const val SINGULARITY_OD = "singularity"

    override fun preInit(machine: DynamicMachine) {
        val orenames = ObjectArrayList<String>()
        val singularitys = Object2LongOpenHashMap<String>()

        for (ench in CraftTweakerAPI.oreDict.entries) {
            val odName = ench.name
            if (odName.startsWith(ORE_OD)) {
                if (ench.isEmpty) continue
                val ore: String = odName.substring(ORE_OD.length)
                if (ore == "Aluminum") continue
                val ingot = CraftTweakerAPI.oreDict.get("ingot$ore")
                val gem = CraftTweakerAPI.oreDict.get("gem$ore")
                val dust = CraftTweakerAPI.oreDict.get("dust$ore")
                val rawOre = CraftTweakerAPI.oreDict.get("rawOre$ore")
                val rawOreGem = CraftTweakerAPI.oreDict.get("rawOreGem$ore")
                val singularity = CraftTweakerAPI.oreDict.get(SINGULARITY_OD + ore)
                var ores: IOreDictEntry? = null
                if (!ingot.isEmpty) {
                    ores = ingot
                } else if (!gem.isEmpty) {
                    ores = gem
                } else if (!dust.isEmpty) {
                    ores = dust
                }
                if (ores != null) {
                    orenames.add("Ore$ore")
                    val rec0 = RecipeBuilder.newBuilder("space_Ore$ore", "space_generator", 20, 2)
                        .addInput(ench)
                        .addPreCheckHandler { event: RecipeCheckEvent? ->
                            val ctrl = event!!.getController()
                            val data = ctrl.customDataTag
                            val hxzt = data.getByte("hxzt")
                            if (hxzt.toInt() != 1) {
                                event.setFailed("novaeng.space_generator.failed.item.input")
                            }
                        }
                        .addFactoryFinishHandler { event: FactoryRecipeFinishEvent? ->
                            val ctrl = event!!.getController()
                            val data = ctrl.customDataTag
                            val Ore1 = data.getLong("Ore$ore")
                            val thread = event.factoryRecipeThread
                            val bx = thread.getActiveRecipe().parallelism

                            data.setLong("Ore$ore", Ore1 + bx)
                            data.setLong("kwzl", data.getLong("kwzl") + bx)
                        }
                        .setMaxThreads(1)
                        .setParallelized(true)
                        .addRecipeTooltip(*StringUtils.getTexts("novaeng.space_generator.recipe.ore.tooltips"))
                    if (!singularity.isEmpty) rec0.addOutput(singularity.firstItem).setChance(0f)
                        .setLore("novaeng.space_generator.recipe.ore.output.tooltips")
                    rec0.addOutput(CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.name)))
                        .setChance(0f).setLore("novaeng.space_generator.recipe.ore.output.tooltips").build()
                    if (!rawOre.isEmpty) {
                        val rec1 = RecipeBuilder.newBuilder("space_Ore1$ore", "space_generator", 20, 2)
                            .addInput(rawOre.amount(3))
                            .addPreCheckHandler { event: RecipeCheckEvent? ->
                                val ctrl = event!!.getController()
                                val data = ctrl.customDataTag
                                val hxzt = data.getByte("hxzt")
                                if (hxzt.toInt() != 1) {
                                    event.setFailed("novaeng.space_generator.failed.item.input")
                                }
                            }
                            .addFactoryFinishHandler { event: FactoryRecipeFinishEvent? ->
                                val ctrl = event!!.getController()
                                val data = ctrl.customDataTag
                                val Ore1 = data.getLong("Ore$ore")
                                val thread = event.factoryRecipeThread
                                val bx = thread.getActiveRecipe().parallelism

                                data.setLong("Ore$ore", Ore1 + (bx * 2L))
                                data.setLong("kwzl", data.getLong("kwzl") + (bx * 2L))
                            }
                            .setMaxThreads(1)
                            .setParallelized(true)
                            .addRecipeTooltip(*StringUtils.getTexts("novaeng.space_generator.recipe.raw_ore.tooltips"))
                        if (!singularity.isEmpty) rec1.addOutput(singularity.firstItem).setChance(0f)
                            .setLore("novaeng.space_generator.recipe.ore.output.tooltips")
                        rec1.addOutput(
                            CraftTweakerMC.getIItemStack(
                                OreHandler.OreDictHelper.getPriorityItemFromOreDict(
                                    ores.name
                                )
                            )
                        ).setChance(0f)
                            .setLore("novaeng.space_generator.recipe.ore.output.tooltips")
                            .build()
                    }
                    if (!rawOreGem.isEmpty) {
                        val rec1 = RecipeBuilder.newBuilder("space_Ore1$ore", "space_generator", 20, 2)
                            .addInput(rawOreGem)
                            .addPreCheckHandler { event: RecipeCheckEvent? ->
                                val ctrl = event!!.getController()
                                val data = ctrl.customDataTag
                                val hxzt = data.getByte("hxzt")
                                if (hxzt.toInt() != 1) {
                                    event.setFailed("novaeng.space_generator.failed.item.input")
                                }
                            }
                            .addFactoryFinishHandler { event: FactoryRecipeFinishEvent? ->
                                val ctrl = event!!.getController()
                                val data = ctrl.customDataTag
                                val Ore1 = data.getLong("Ore$ore")
                                val thread = event.factoryRecipeThread
                                val bx = thread.getActiveRecipe().parallelism

                                data.setLong("Ore$ore", Ore1 + bx)
                                data.setLong("kwzl", data.getLong("kwzl") + bx)
                            }
                            .setMaxThreads(1)
                            .setParallelized(true)
                            .addRecipeTooltip(*StringUtils.getTexts("novaeng.space_generator.recipe.gem.tooltips"))
                        if (!singularity.isEmpty) rec1.addOutput(singularity.firstItem).setChance(0f)
                            .setLore("novaeng.space_generator.recipe.ore.output.tooltips")
                        rec1.addOutput(
                            CraftTweakerMC.getIItemStack(
                                OreHandler.OreDictHelper.getPriorityItemFromOreDict(
                                    ores.name
                                )
                            )
                        ).setChance(0f)
                            .setLore("novaeng.space_generator.recipe.ore.output.tooltips")
                            .build()
                    }
                }
            }
            if (odName.startsWith(SINGULARITY_OD) && odName != SINGULARITY_OD) {
                val singularityname: String = odName.substring(SINGULARITY_OD.length)
                val s = OreHandler.OreDictHelper.getPriorityItemFromOreDict(odName)
                if (s.isEmpty) continue
                val r = AvaritiaRecipeManager.getCompressorRecipeFromResult(s) ?: continue

                singularityOperation(CraftTweakerMC.getIItemStack(s), singularityname)

                val count = r.cost
                if (singularityname != "Quartz") {
                    singularitys[singularityname] = count * 9L
                } else {
                    singularitys[singularityname] = count * 4L
                }
            }
        }

        oreProcessing(orenames, singularitys)

        machine.addCoreThread(
            FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.core").addRecipe("hxzk1")
                .addRecipe("hxzk2").addRecipe("hxzk3").addRecipe("hxzk4")
        )
        machine.addCoreThread(
            FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.energy").addRecipe("space_energy1")
                .addRecipe("space_energy2")
        )
        machine.addCoreThread(
            FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.fluid").addRecipe("space_fluid")
        )

        machine.setInternalParallelism(2000000000)
        machine.setMaxThreads(0)
        for (i in 0..9) {
            machine.addCoreThread(FactoryRecipeThread.createCoreThread("novaeng.space_generator.thread.item.input.$i"))
        }
    }

    override fun getRegistryName(): ResourceLocation {
        return REGISTRY_NAME
    }

    private fun oreProcessing(orenames: MutableList<String>, singularitys: Object2LongMap<String>) {
        var rec = RecipeBuilder.newBuilder("hxzk3", "space_generator", 1, 99999)
            .addPreCheckHandler { event ->
                val ctrl = event!!.getController()
                val data = ctrl.customDataTag
                val hxzt = data.getByte("hxzt")
                if (hxzt.toInt() != 3) {
                    event.setFailed("novaeng.space_generator.failed.no_output")
                }
            }

        for (orename in orenames) {
            val singularityname: String = orename.substring(ORE_OD.length)
            val singularity = CraftTweakerAPI.oreDict.get(SINGULARITY_OD + singularityname)
            if (!singularity.isEmpty) {
                rec.addOutput(singularity.firstItem)
                    .addItemModifier { ctrl: IMachineController, item: IItemStack ->
                        output(
                            orename,
                            ctrl,
                            item,
                            singularitys[singularityname]!!
                        )
                    }
            }

            val Ore: String = orename.substring(ORE_OD.length)
            val ingot = CraftTweakerAPI.oreDict.get("ingot$Ore")
            val gem = CraftTweakerAPI.oreDict.get("gem$Ore")
            val dust = CraftTweakerAPI.oreDict.get("dust$Ore")
            var ores: IOreDictEntry? = null
            if (!ingot.isEmpty) {
                ores = ingot
            } else if (!gem.isEmpty) {
                ores = gem
            } else if (!dust.isEmpty) {
                ores = dust
            }
            if (ores != null) {
                rec = rec.addOutput(
                    CraftTweakerMC.getIItemStack(
                        OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.name)
                    )
                ).addItemModifier { ctrl: IMachineController, oldItem: IItemStack ->
                    output(
                        orename,
                        ctrl,
                        oldItem,
                        0
                    )
                }
            }
        }
        rec.setParallelized(false)
            .addFactoryFinishHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                data.setByte("hxzt", 0.toByte())
            }
            .setLoadJEI(false)
            .setThreadName("novaeng.space_generator.thread.core")
            .build()
    }

    private fun output(i: String, ctrl: IMachineController, oldItem: IItemStack, qdsl: Long): IItemStack? {
        val data = ctrl.controller.customDataTag
        val power = when (i) {
            "OreLapis" -> 5
            "OreRedstone" -> 6
            "OreCertusQuartz", "OreChargedCertusQuartz" -> 2
            else -> 1
        }
        val oreAmount = data.getDouble(i)
        val efficiencyFactor1 = data.getFloat("cq1")
        val efficiencyFactor2 = data.getFloat("cq2")

        if (efficiencyFactor1 == 0.0f || efficiencyFactor2 == 0.0f) {
            data.setLong(i, 0)
            return oldItem.amount(0)
        }

        var stackSizeChange: Long = 0

        if (oreAmount != 0.0) {
            val totalFactor = (4.0f * efficiencyFactor1 * efficiencyFactor2 * power).toDouble()

            if (qdsl != 0L) {
                val maxProcessableByQdsl = qdsl / totalFactor

                if (oreAmount > maxProcessableByQdsl) {
                    val requiredRatio = oreAmount / maxProcessableByQdsl
                    val addedCount = floor(requiredRatio).toLong()
                    stackSizeChange += addedCount

                    val remainingOre = oreAmount - (qdsl * addedCount) / totalFactor
                    data.setLong(i, remainingOre.toLong())
                }
            } else {
                val addedCount = oreAmount * totalFactor
                stackSizeChange += addedCount.toLong()
                data.setLong(i, 0)
            }
        }

        return oldItem.amount(if (stackSizeChange >= Int.MAX_VALUE) Int.MAX_VALUE else stackSizeChange.toInt())
    }

    private fun singularityOperation(singularity: IItemStack?, singularityname: String) {
        val arg0 =
            AvaritiaRecipeManager.getCompressorRecipeFromResult(CraftTweakerMC.getItemStack(singularity)).cost
        val arg2 = (0.7 * arg0).roundToLong()
        val arg1: Int = if (singularityname == "Quartz") {
            4
        } else {
            9
        }
        val ingot = CraftTweakerAPI.oreDict.get("ingot$singularityname")
        val nugget = CraftTweakerAPI.oreDict.get("nugget$singularityname")
        val block = CraftTweakerAPI.oreDict.get("block$singularityname")
        val gem = CraftTweakerAPI.oreDict.get("gem$singularityname")
        val dust = CraftTweakerAPI.oreDict.get("dust$singularityname")
        val ores: IOreDictEntry
        if (!ingot.isEmpty) {
            ores = ingot
        } else if (!gem.isEmpty) {
            ores = gem
        } else {
            ores = dust
        }
        RecipeBuilder.newBuilder("space_singularity1$singularityname", "space_generator", 20, 3)
            .addEnergyPerTickInput(100000000)
            .addIngredientArrayInput(
                IngredientArrayBuilder.newBuilder().addIngredients(
                    block.amount(arg0),
                    ores.amount(arg1 * arg0),
                    nugget.amount(9 * arg1 * arg0)
                )
            )
            .addPreCheckHandler { event: RecipeCheckEvent? ->
                val ctrl = event!!.getController()
                val data = ctrl.customDataTag
                val hxzt = data.getInteger("hxzt")
                if (hxzt != 2) {
                    event.setFailed("novaeng.space_generator.failed.singularity")
                }
            }
            .addOutput(singularity)
            .setMaxThreads(1)
            .setParallelized(true)
            .addRecipeTooltip(
                "novaeng.space_generator.tooltip0",
                "novaeng.space_generator.tooltip1"
            )
            .build()
        RecipeBuilder.newBuilder("space_singularity2$singularityname", "space_generator", 20, 3)
            .addEnergyPerTickInput(100000000)
            .addInput(singularity)
            .addPreCheckHandler { event: RecipeCheckEvent? ->
                val ctrl = event!!.getController()
                val data = ctrl.customDataTag
                val hxzt = data.getInteger("hxzt")
                if (hxzt != 2) {
                    event.setFailed("novaeng.space_generator.failed.singularity")
                }
            }
            .addOutputs(
                CraftTweakerMC.getIItemStack(OreHandler.OreDictHelper.getPriorityItemFromOreDict(ores.name))
                    .amount((arg1 * arg2).toInt())
            )
            .setMaxThreads(1)
            .setParallelized(true)
            .addRecipeTooltip(
                "novaeng.space_generator.tooltip0",
                "novaeng.space_generator.tooltip1"
            )
            .build()
    }
}