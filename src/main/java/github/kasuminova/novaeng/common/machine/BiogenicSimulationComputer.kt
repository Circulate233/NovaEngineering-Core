package github.kasuminova.novaeng.common.machine

import appeng.util.Platform
import crafttweaker.CraftTweakerAPI
import crafttweaker.api.item.IItemStack
import crafttweaker.api.minecraft.CraftTweakerMC
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent
import github.kasuminova.mmce.common.helper.IMachineController
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireComputationPoint
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper
import github.kasuminova.novaeng.common.util.RecipePrimerEx.setLore
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread
import mustapelto.deepmoblearning.common.DMLRegistry
import mustapelto.deepmoblearning.common.util.DataModelHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import kotlin.math.max

//TODO:处理硬编码
object BiogenicSimulationComputer : MachineSpecial {

    private val clay = CraftTweakerMC.getIItemStack(ItemStack(DMLRegistry.ITEM_POLYMER_CLAY))
    private val blank = CraftTweakerMC.getIItemStack(ItemStack(DMLRegistry.ITEM_DATA_MODEL_BLANK))
    private const val MACHINEID = "biogenic_simulation_computer"
    val REGISTRY_NAME = ResourceLocation(ModularMachinery.MODID, MACHINEID)
    private val inscriberModels = arrayOf<String?>(
        "数位演算模块-α",
        "数位演算模块-β",
        "数位演算模块-δ",
        "数位演算模块-Ω"
    )

    override fun preInit(machine: DynamicMachine) {
        MachineModifier.setMaxThreads(MACHINEID, 0)
        for (i in inscriberModels) {
            MachineModifier.addCoreThread(MACHINEID, FactoryRecipeThread.createCoreThread(i))
        }
        HyperNetHelper.proxyMachineForHyperNet(MACHINEID)

        for (i in inscriberModels.indices) {
            val ysqname = "ysqname$i"
            val ysqddcs = "ysqddcs$i"
            val prepare = "prepare$i"

            RecipeBuilder.newBuilder("moxll$i", MACHINEID, 1, 0)
                .addItemInput(CraftTweakerAPI.oreDict.get("dataModel")).setTag("dataModel")
                .setNBTChecker { ctrl: IMachineController?, iitem: IItemStack? ->
                    val item = CraftTweakerMC.getItemStack(iitem)
                    val data = ctrl!!.controller.customDataTag

                    data.setTag(prepare, item.writeToNBT(NBTTagCompound()))
                    true
                }
                .addPreCheckHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    if (data.hasKey(ysqname)) {
                        event.setFailed("数据模块注入完成,可以开始演算")

                        for (ii in inscriberModels.indices) {
                            data.removeTag("prepare$ii")
                        }
                    }
                }
                .addFactoryStartHandler { event: FactoryRecipeStartEvent ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    if (!data.hasKey(ysqname)) {
                        val itemData = data.getTag(prepare) as NBTTagCompound
                        val preItem = ItemStack(itemData)
                        val tier = DataModelHelper.getTier(preItem)
                        val dataCount = DataModelHelper.getCurrentTierDataCount(preItem)
                        val tierend = if (tier <= 1) 32 * tier + dataCount else dataCount + (tier - 1) * 10000 + 32

                        data.setTag(ysqname, itemData)
                        data.setLong(ysqddcs, tierend.toLong())

                        for (ii in inscriberModels.indices) {
                            data.removeTag("prepare$ii")
                        }
                    }
                }
                .addOutput(blank)
                .setParallelized(false)
                .addRecipeTooltip("将数据模型写入数位演算模块", "请将数据模型放入控制器正上方的微型物品输入仓中")
                .setThreadName(inscriberModels[i])
                .setLoadJEI(i == 0)
                .build()

            RecipeBuilder.newBuilder("moni$i", MACHINEID, 60, 0)
                .addEnergyPerTickInput(1000000)
                .addItemInput(clay)
                .addPreCheckHandler { event: RecipeCheckEvent ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val parallelism = max(data.getInteger("parallelism"), 1)

                    if (!data.hasKey(ysqname)) {
                        event.setFailed("没有数据模型！")
                        return@addPreCheckHandler
                    }
                    event.activeRecipe.maxParallelism = parallelism
                }
                .addFactoryStartHandler { event: FactoryRecipeStartEvent ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val ysqddcss = data.getInteger(ysqddcs)
                    val bl = event.factoryRecipeThread
                    if (ysqddcss < 32) {
                        bl.addModifier(
                            "duration",
                            RecipeModifierBuilder.create("modularmachinery:duration", "input", 60f, 1, false).build()
                        )
                        bl.addModifier(
                            "energy",
                            RecipeModifierBuilder.create("modularmachinery:energy", "input", 20f, 1, false).build()
                        )
                    }
                }
                .addItemOutput(CraftTweakerAPI.oreDict.get("pristine"))
                .addItemModifier { ctrl: IMachineController?, Item: IItemStack? ->
                    outputPristineMatter(
                        ctrl!!,
                        ysqname,
                        ysqddcs
                    )
                }
                .addItemOutput(CraftTweakerAPI.oreDict.get("livingMatter"))
                .addItemModifier { ctrl: IMachineController, Item: IItemStack ->
                    outputLivingMatter(
                        ctrl,
                        ysqname
                    )
                }
                .addFactoryFinishHandler { event: FactoryRecipeFinishEvent ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val bx = event.factoryRecipeThread.getActiveRecipe().parallelism
                    data.setLong(ysqddcs, data.getLong(ysqddcs) + bx)
                }
                .addRecipeTooltip(
                    "使用数位演算模块进行模拟,并且输出物质",
                    "概率继承自模拟室,并且每个等级额外提高2%",
                    "等级为0的模型需要60倍的时间和20倍能量来进行初步推算"
                )
                .setThreadName(inscriberModels[i])
                .setLoadJEI(i == 0)
                .requireComputationPoint(100.0f)
                .build()

            RecipeBuilder.newBuilder("mxdc$i", MACHINEID, 1)
                .addItemInput(blank)
                .addPreCheckHandler { event: RecipeCheckEvent ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    if (!data.hasKey(ysqname)) {
                        event.setFailed("没有可以导出的数据")
                    }
                }
                .addOutput(CraftTweakerAPI.oreDict.get("dataModel"))
                .setLore("§6提取出写入的模型")
                .addItemModifier { ctrl: IMachineController?, Item: IItemStack? ->
                    outputdata(
                        ctrl!!,
                        ysqname,
                        ysqddcs
                    )
                }
                .setParallelized(false)
                .addRecipeTooltip(
                    "将数据模型从数位演算模块导出",
                    "会先从哪个数据里导出？谁知道呢,试试不就知道了"
                ).setThreadName(inscriberModels[i])
                .setLoadJEI(i == 0)
                .build()
        }

        machine.addMachineEventHandler(
            ControllerGUIRenderEvent::class.java
        ) { event: ControllerGUIRenderEvent ->
            val ctrl = event.getController()
            val data = ctrl.customDataTag
            val info = ArrayList<String?>()

            for (i in inscriberModels.indices) {
                val itemData = data.getTag("ysqname$i")
                val ysqname: String?
                if (itemData == null) {
                    ysqname = "暂无"
                } else {
                    val item = ItemStack(itemData as NBTTagCompound)
                    ysqname = item.item.getItemStackDisplayName(item).replace("[(].*".toRegex(), "")
                }
                val ysqddcs = data.getLong("ysqddcs$i")
                info.add("当前记录模型：$ysqname")
                info.add("当前迭代次数：$ysqddcs")
            }
            @Suppress("UsePropertyAccessSyntax")
            event.setExtraInfo(*info.toTypedArray())
        }
    }

    override fun getRegistryName(): ResourceLocation {
        return REGISTRY_NAME
    }

    private fun outputLivingMatter(ctrl: IMachineController, ysqnamess: String): IItemStack? {
        val data = ctrl.controller.customDataTag
        val name = data.getTag(ysqnamess)
        val item = DataModelHelper.getDataModelMetadata(ItemStack(name as NBTTagCompound))

        return item.map {
            CraftTweakerMC.getIItemStack(it.livingMatter)
        }.orElse(null)
    }

    private fun outputPristineMatter(ctrl: IMachineController, ysqnamess: String, ysqddcss: String): IItemStack? {
        val data = ctrl.controller.customDataTag
        val name = data.getTag(ysqnamess) ?: return null
        val ysqddcs = data.getLong(ysqddcss)
        val world = ctrl.controller.getWorld()
        val Random = world.rand.nextInt(99) + 1
        val item = DataModelHelper.getDataModelMetadata(ItemStack(name as NBTTagCompound))
        val itemsl: Boolean = if (ysqddcs >= 32) {
            if (ysqddcs < 10032) {
                6 >= Random
            } else if (ysqddcs < 20032) {
                12 >= Random
            } else if (ysqddcs < 30032) {
                14 >= Random
            } else {
                20 >= Random
            }
        } else {
            false
        }

        return if (item.isPresent) {
            if (itemsl) {
                CraftTweakerMC.getIItemStack(item.get().pristineMatter)
            } else {
                null
            }
        } else {
            clay.amount(1)
        }
    }

    private fun outputdata(ctrl: IMachineController, ysqnamess: String, ysqddcss: String): IItemStack {
        val data = ctrl.controller.customDataTag
        val name = data.getCompoundTag(ysqnamess)
        val ysqddcs = data.getLong(ysqddcss)

        var tiers: Int
        var dataCounts: Int

        if (ysqddcs < 32) {
            tiers = 0
            dataCounts = ysqddcs.toInt()
        } else if (ysqddcs < 10032) {
            tiers = 1
            dataCounts = (ysqddcs - 32).toInt()
        } else if (ysqddcs < 20032) {
            tiers = 2
            dataCounts = (ysqddcs - 10032).toInt()
        } else if (ysqddcs < 30032) {
            tiers = 3
            dataCounts = (ysqddcs - 20032).toInt()
        } else {
            tiers = 4
            dataCounts = if (ysqddcs > 2000000000) {
                2000000000
            } else {
                ysqddcs.toInt()
            }
        }

        data.removeTag(ysqnamess)
        data.removeTag(ysqddcss)

        val item = ItemStack(name)
        val nbt = Platform.openNbtData(item)
        nbt.setLong("totalSimulationCount", ysqddcs)
        nbt.setInteger("tier", tiers)
        nbt.setInteger("dataCount", dataCounts)

        return CraftTweakerMC.getIItemStack(item)
    }
}