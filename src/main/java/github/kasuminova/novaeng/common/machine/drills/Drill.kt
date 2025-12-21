package github.kasuminova.novaeng.common.machine.drills

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralMix
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralWorldInfo
import blusunrize.immersiveengineering.common.Config
import crafttweaker.CraftTweakerAPI.itemUtils
import crafttweaker.CraftTweakerAPI.oreDict
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.minecraft.CraftTweakerMC
import crafttweaker.api.oredict.IOreDictEntry
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireComputationPoint
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireResearch
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper
import github.kasuminova.novaeng.common.handler.OreHandler
import github.kasuminova.novaeng.common.machine.MachineSpecial
import github.kasuminova.novaeng.common.machine.drills.Drill.Type.RANGE
import github.kasuminova.novaeng.common.machine.drills.Drill.Type.SINGLE
import github.kasuminova.novaeng.common.util.IDataUtils
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import mcjty.theoneprobe.api.IProbeHitData
import mcjty.theoneprobe.api.IProbeInfo
import mcjty.theoneprobe.api.ProbeMode
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

abstract class Drill : MachineSpecial {

    companion object {
        protected val tqsz: IntArray = intArrayOf(-1, 0, 1)
        protected val tqdzb = Object2IntOpenHashMap<String>()
        val errorStone: IItemStack
        val stone: IItemStack = itemUtils.getItem("minecraft:stone", 0)
        val circuit_0: IOreDictEntry = oreDict.get("programmingCircuit")
        val dust: IOreDictEntry = oreDict.get("itemPulsatingPowder")
        protected var basicMineralMix: Int
        protected var MMMineralMix: Int

        init {
            var j = 0
            for (i in tqsz) {
                for (ii in tqsz) {
                    tqdzb.put("" + (i + 1) + (ii + 1), ++j)
                }
            }
            basicMineralMix = Config.IEConfig.Machines.excavator_depletion
            MMMineralMix = basicMineralMix * 3

            val item = ItemStack(Blocks.STONE)
            val nbt = NBTTagCompound()
            nbt.setByte("error", 1.toByte())
            item.tagCompound = nbt
            errorStone = CraftTweakerMC.getIItemStack(item)
        }

        protected fun chunkCoord(posValue: Int): Int {
            return posValue shr 4
        }

        private fun getOreOutput(ctrl: TileMultiblockMachineController, pos: BlockPos, worldid: Int): IItemStack {
            return getOreOutput(ctrl, pos, worldid, 0, 0)
        }

        private fun getOreOutput(
            ctrl: TileMultiblockMachineController,
            pos: BlockPos,
            worldid: Int,
            k: Int,
            kk: Int
        ): IItemStack {
            val world = DimensionManager.getWorld(worldid) ?: return errorStone.mutable().copy()
            val data = ctrl.customDataTag
            val research_progress = data.getByte("research_progress")
            val components_amount = data.getByte("components_amount")
            val sfsh = max(
                (if (data.hasKey("sfsh")) data.getInteger("sfsh") else 10000) - (1000 * research_progress.toDouble()
                    .pow(1.6)), 0.0
            )
            val bxs = data.getInteger("bxs" + (k + 1) + (kk + 1)) * (1 + components_amount.toDouble().pow(2.5))
            val component_raw_ore = data.getByte("additional_component_raw_ore")
            var random = ctrl.getWorld().rand.nextInt(10000)
            val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                world,
                (chunkCoord(pos.x) + k),
                (chunkCoord(pos.z) + kk)
            )
            if (sfsh < 10000) {
                if (sfsh > random) {
                    worldInfo.depletion += bxs.toInt()
                    data.setInteger("depletion" + (k + 1) + (kk + 1), worldInfo.depletion)
                    ctrl.customDataTag = data
                }
            } else {
                val sl = floor(1.0f * sfsh) / 10000
                worldInfo.depletion += (bxs * sl).toInt()
                if ((sfsh - (10000 * sl)) > random) {
                    worldInfo.depletion += bxs.toInt()
                }
                data.setInteger("depletion" + (k + 1) + (kk + 1), worldInfo.depletion)
                ctrl.customDataTag = data
            }
            val mineral: MineralMix? = getUsableMix(worldInfo)
            if (mineral != null) {
                val iore = mineral.getRandomOre(world.rand)
                if (component_raw_ore.toInt() == 1) {
                    val rawore = OreHandler.getRawOre(iore)
                    if (rawore == null) {
                        return OreHandler.getOre(iore)
                    } else {
                        random = world.rand.nextInt(6)
                        return rawore.amount(max(random, 1))
                    }
                }
                return OreHandler.getOre(iore)
            } else {
                data.setString("kmm" + (k + 1) + (kk + 1), "empty")
                return stone
            }
        }

        private fun getCcrystalOutput(ctrl: TileMultiblockMachineController): IItemStack {
            val data = ctrl.customDataTag
            val world = ctrl.getWorld()
            val research_progress = data.getByte("research_progress")
            if (research_progress > 0) {
                val cjc: MutableList<IItemStack> = ReferenceArrayList()
                cjc.add(itemUtils.getItem("environmentaltech:litherite_crystal", 0))
                cjc.add(itemUtils.getItem("environmentaltech:erodium_crystal", 0))
                if (research_progress > 1) {
                    cjc.add(itemUtils.getItem("environmentaltech:kyronite_crystal", 0))
                    cjc.add(itemUtils.getItem("environmentaltech:pladium_crystal", 0))
                    if (research_progress > 2) {
                        cjc.add(itemUtils.getItem("environmentaltech:ionite_crystal", 0))
                        if (research_progress > 3) {
                            cjc.add(itemUtils.getItem("environmentaltech:aethium_crystal", 0))
                        }
                    }
                }
                val random = world.rand.nextInt(cjc.size)
                return cjc[random]
            } else {
                return stone
            }
        }

        private fun getUsableMix(worldInfo: MineralWorldInfo): MineralMix? {
            return worldInfo.mineralOverride ?: worldInfo.mineral
        }
    }

    private val resourceLocation = ResourceLocation(ModularMachinery.MODID, this.getMachineName())

    override fun preInit(machine: DynamicMachine) {
        machine.addMachineEventHandler(
            MachineStructureUpdateEvent::class.java
        ) {
            val controller = it.getController()
            controller.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC)
        }
        regUpgrade(machine)
        regRecipe(machine)
        if (NovaEngineeringCore.proxy.isClient) regGui(machine)
    }

    protected abstract fun getCoreTheardName(): String

    protected val recipeTime: Int
        get() = (120 * this.getRecipeTimeMultiple()).toInt()

    protected fun getRecipeTime(isAdvanced: Boolean): Int {
        var out = 240 * this.getRecipeTimeMultiple()
        if (isAdvanced) out *= this.getAdvancedRecipeTimeMultiple()
        return out.toInt()
    }

    protected open fun getAdvancedRecipeTimeMultiple(): Float {
        return 1f
    }

    protected open fun getRecipeTimeMultiple(): Float {
        return 1f
    }

    protected abstract fun getMachineName(): String

    protected abstract fun getType(): Type

    protected abstract fun getBaseEnergy(): Long

    protected val parallelism: Int
        get() = when (this.getType()) {
            RANGE -> 64
            SINGLE -> 4
        }

    protected open fun isDimensional(): Boolean {
        return false
    }

    private val emptyIngredient = arrayOf<IIngredient>()

    protected open fun getExIngredient(): Array<IIngredient> {
        return emptyIngredient
    }

    override fun getRegistryName(): ResourceLocation {
        return resourceLocation
    }

    private fun regRecipe(machine: DynamicMachine) {
        when (this.getType()) {
            SINGLE -> {
                val threadName = this.getCoreTheardName()
                val thread = FactoryRecipeThread.createCoreThread(threadName)
                machine.addCoreThread(thread)
                var recipeName: String
                val r0 = RecipeBuilder.newBuilder(
                    (this.getMachineName() + "_ex_01").also { recipeName = it },
                    this.getMachineName(),
                    this.recipeTime, 1
                )
                    .setLoadJEI(false)
                    .addEnergyPerTickInput(this.getBaseEnergy())
                    .addInput(circuit_0).setChance(0f)
                    .addPreCheckHandler { event ->
                        val ctrl = event.getController()
                        val data = ctrl.customDataTag
                        if (!data.hasKey("pos")) {
                            event.setFailed("novaeng.drill.failed.pos")
                            return@addPreCheckHandler
                        }
                        val world: World?
                        if (this.isDimensional()) {
                            val poss = data.getIntArray("pos")
                            world = DimensionManager.getWorld(poss[3])
                        } else {
                            world = ctrl.getWorld()
                        }
                        if (world == null) {
                            event.setFailed("novaeng.drill.failed.mineral")
                            return@addPreCheckHandler
                        }
                        val kmm = data.getString("kmm11")
                        val depletion = data.getInteger("depletion11")
                        if (kmm == "empty") {
                            event.setFailed("novaeng.drill.failed.mineral.empty")
                            return@addPreCheckHandler
                        }
                        if (depletion >= (MMMineralMix - 100)) {
                            event.setFailed("novaeng.drill.failed.mineral.depletion")
                            data.setInteger("depletion11", MMMineralMix)
                            data.setString("kmm11", "empty")
                            return@addPreCheckHandler
                        }
                        ctrl.customDataTag = data
                    }
                    .addFactoryStartHandler { event ->
                        val ctrl = event.getController()
                        val data = ctrl.customDataTag
                        val kmm = data.getString("kmm11")
                        val x: Int
                        val z: Int
                        val world: World
                        if (this.isDimensional()) {
                            val poss = data.getIntArray("pos")
                            x = poss[0]
                            z = poss[2]
                            world = DimensionManager.getWorld(poss[3])
                        } else {
                            x = ctrl.getPos().x
                            z = ctrl.getPos().z
                            world = ctrl.getWorld()
                        }
                        val bxs = event.factoryRecipeThread.getActiveRecipe().parallelism
                        val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                            world,
                            chunkCoord(x),
                            chunkCoord(z)
                        )
                        val mineral: MineralMix? = getUsableMix(worldInfo)
                        if (mineral != null) {
                            if (kmm != mineral.name) {
                                data.setString("kmm11", mineral.name)
                            }
                        } else {
                            data.setString("kmm11", "empty")
                        }
                        data.setInteger("bxs11", bxs)
                        data.setInteger("sfsh", 8000)
                    }
                if (this.getExIngredient().isNotEmpty()) {
                    r0.addInputs(*this.getExIngredient())
                }
                for (i in 0..3) {
                    r0.addOutput(stone)
                        .addItemModifier { ctrl, item ->
                            if (this.isDimensional()) {
                                val poss = ctrl.controller.customDataTag.getIntArray("pos")
                                val pos = BlockPos(poss[0], poss[1], poss[2])
                                return@addItemModifier getOreOutput(ctrl.controller, pos, poss[3])
                            } else {
                                return@addItemModifier getOreOutput(
                                    ctrl.controller,
                                    ctrl.controller.getPos(),
                                    ctrl.iWorld.dimension
                                )
                            }
                        }
                }
                r0.requireComputationPoint(1.5f)
                    .addOutput(stone)
                    .addItemModifier { ctrl, item ->
                        getCcrystalOutput(ctrl.controller)
                    }
                    .setChance(0.1f)
                    .setParallelized(false)
                    .setThreadName(threadName)
                    .build()
                thread.addRecipe(recipeName)

                val r1 = RecipeBuilder.newBuilder(
                    (this.getMachineName() + "_ex_11").also { recipeName = it },
                    this.getMachineName(),
                    this.recipeTime, 1
                )
                    .setLoadJEI(false)
                    .addEnergyPerTickInput(this.getBaseEnergy() * 2)
                    .addInput(itemUtils.getItem("thermalinnovation:drill", 4)).setChance(0f)
                    .addPreCheckHandler { event ->
                        val ctrl = event.getController()
                        val data = ctrl.customDataTag
                        if (!data.hasKey("pos")) {
                            event.setFailed("novaeng.drill.failed.pos")
                            return@addPreCheckHandler
                        }
                        val world: World?
                        if (this.isDimensional()) {
                            val poss = data.getIntArray("pos")
                            world = DimensionManager.getWorld(poss[3])
                        } else {
                            world = ctrl.getWorld()
                        }
                        if (world == null) {
                            event.setFailed("novaeng.drill.failed.mineral")
                            return@addPreCheckHandler
                        }
                        val kmm = data.getString("kmm11")
                        val depletion = data.getInteger("depletion11")
                        if (kmm == "empty") {
                            event.setFailed("novaeng.drill.failed.mineral.empty")
                            return@addPreCheckHandler
                        }
                        if (depletion >= (MMMineralMix - 100)) {
                            event.setFailed("novaeng.drill.failed.mineral.depletion")
                            data.setInteger("depletion11", MMMineralMix)
                            data.setString("kmm11", "empty")
                            return@addPreCheckHandler
                        }
                        ctrl.customDataTag = data
                    }
                    .addFactoryStartHandler { event ->
                        val ctrl = event.getController()
                        val data = ctrl.customDataTag
                        val kmm = data.getString("kmm11")
                        val x: Int
                        val z: Int
                        val world: World
                        if (this.isDimensional()) {
                            val poss = data.getIntArray("pos")
                            x = poss[0]
                            z = poss[2]
                            world = DimensionManager.getWorld(poss[3])
                        } else {
                            x = ctrl.getPos().x
                            z = ctrl.getPos().z
                            world = ctrl.getWorld()
                        }
                        val bxs = event.factoryRecipeThread.getActiveRecipe().parallelism
                        val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                            world,
                            chunkCoord(x),
                            chunkCoord(z)
                        )
                        val mineral: MineralMix? = getUsableMix(worldInfo)
                        if (mineral != null) {
                            if (kmm != mineral.name) {
                                data.setString("kmm11", mineral.name)
                            }
                        } else {
                            data.setString("kmm11", "empty")
                        }
                        data.setInteger("bxs11", bxs)
                        data.setInteger("sfsh", 9000)
                    }
                if (this.getExIngredient().isNotEmpty()) {
                    r1.addInputs(*this.getExIngredient())
                }
                for (i in 0..3) {
                    r1.addOutput(stone)
                        .addItemModifier { ctrl, item ->
                            if (this.isDimensional()) {
                                val poss = ctrl.controller.customDataTag.getIntArray("pos")
                                val pos = BlockPos(poss[0], poss[1], poss[2])
                                return@addItemModifier getOreOutput(ctrl.controller, pos, poss[3])
                            } else {
                                return@addItemModifier getOreOutput(
                                    ctrl.controller,
                                    ctrl.controller.getPos(),
                                    ctrl.iWorld.dimension
                                )
                            }
                        }
                }
                r1.requireComputationPoint(1.5f)
                r1.addOutput(stone)
                    .addItemModifier { ctrl, item ->
                        getCcrystalOutput(ctrl.controller)
                    }.setChance(0.1f)
                    .setParallelized(true)
                    .setThreadName(threadName)
                    .build()
                thread.addRecipe(recipeName)
            }

            RANGE -> {
                for (i in tqsz) {
                    val k = i + 1
                    for (ii in tqsz) {
                        val kk = ii + 1
                        val threadName = this.getCoreTheardName() + "." + tqdzb.getInt(k.toString() + kk)
                        val thread = FactoryRecipeThread.createCoreThread(threadName)
                        machine.addCoreThread(thread)
                        var recipeName: String

                        val r0 = RecipeBuilder.newBuilder(
                            (this.getMachineName() + k + kk).also { recipeName = it },
                            this.getMachineName(), getRecipeTime(false), 1
                        )
                            .setLoadJEI(false)
                            .addEnergyPerTickInput(this.getBaseEnergy())
                            .addPreCheckHandler { event ->
                                val ctrl = event.getController()
                                val data = ctrl.customDataTag
                                val kmm = data.getString("kmm$k$kk")
                                val depletion = data.getInteger("depletion$k$kk")
                                if (kmm == "empty") {
                                    event.setFailed("novaeng.drill.failed.mineral.empty")
                                    return@addPreCheckHandler
                                }
                                if (depletion >= (MMMineralMix - 100)) {
                                    event.setFailed("novaeng.drill.failed.mineral.depletion")
                                    data.setInteger("depletion$k$kk", MMMineralMix)
                                    data.setString("kmm$k$kk", "empty")
                                }
                            }
                            .addFactoryStartHandler { event ->
                                val ctrl = event.getController()
                                val data = ctrl.customDataTag
                                data.getString("kmm$k$kk")
                                val x: Int
                                val z: Int
                                val world: World
                                if (this.isDimensional()) {
                                    val poss = data.getIntArray("pos")
                                    x = poss[0]
                                    z = poss[2]
                                    world = DimensionManager.getWorld(poss[3])
                                } else {
                                    x = ctrl.getPos().x
                                    z = ctrl.getPos().z
                                    world = ctrl.getWorld()
                                }
                                val bxs = event.factoryRecipeThread.getActiveRecipe().parallelism
                                val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                                    world,
                                    (chunkCoord(x) + i),
                                    (chunkCoord(z) + ii)
                                )
                                val mineral: MineralMix? = getUsableMix(worldInfo)
                                if (mineral != null) {
                                    data.setString("kmm$k$kk", mineral.name)
                                } else {
                                    data.setString("kmm$k$kk", "empty")
                                }
                                data.setInteger("sfsh", 8000)
                                data.setInteger("bxs$k$kk", bxs)
                            }
                            .addInput(circuit_0).setChance(0f)
                        for (j in 0..2) {
                            r0.addOutput(stone)
                                .addItemModifier { ctrl, item ->
                                    if (this.isDimensional()) {
                                        val poss = ctrl.controller.customDataTag.getIntArray("pos")
                                        val pos = BlockPos(poss[0], poss[1], poss[2])
                                        return@addItemModifier getOreOutput(ctrl.controller, pos, poss[3])
                                    } else {
                                        return@addItemModifier getOreOutput(
                                            ctrl.controller,
                                            ctrl.controller.getPos(),
                                            ctrl.iWorld.dimension
                                        )
                                    }
                                }
                        }

                        r0.requireComputationPoint(3f)
                        r0.addOutput(stone)
                            .addItemModifier { ctrl, item ->
                                getCcrystalOutput(ctrl.controller)
                            }.setChance(0.035f)
                            .setMaxThreads(1)
                            .setParallelized(false)
                            .setThreadName(threadName)
                            .build()
                        thread.addRecipe(recipeName)

                        val r1 = RecipeBuilder.newBuilder(
                            (this.getMachineName() + "_ex_" + k + kk).also { recipeName = it },
                            this.getMachineName(), getRecipeTime(true), 1
                        )
                            .setLoadJEI(false)
                            .addEnergyPerTickInput(this.getBaseEnergy() * 2)
                            .addPreCheckHandler { event ->
                                val ctrl = event.getController()
                                val data = ctrl.customDataTag
                                val kmm = data.getString("kmm$k$kk")
                                val depletion = data.getInteger("depletion$k$kk")
                                if (kmm == "empty") {
                                    event.setFailed("novaeng.drill.failed.mineral.empty")
                                    return@addPreCheckHandler
                                }
                                if (depletion >= (MMMineralMix - 100)) {
                                    event.setFailed("novaeng.drill.failed.mineral.depletion")
                                    data.setInteger("depletion$k$kk", MMMineralMix)
                                    data.setString("kmm$k$kk", "empty")
                                }
                                event.activeRecipe.maxParallelism = 8
                            }
                            .addFactoryStartHandler { event ->
                                val ctrl = event.getController()
                                val data = ctrl.customDataTag
                                data.getString("kmm$k$kk")
                                val x: Int
                                val z: Int
                                val world: World
                                if (this.isDimensional()) {
                                    val poss = data.getIntArray("pos")
                                    x = poss[0]
                                    z = poss[2]
                                    world = DimensionManager.getWorld(poss[3])
                                } else {
                                    x = ctrl.getPos().x
                                    z = ctrl.getPos().z
                                    world = ctrl.getWorld()
                                }
                                val bxs = event.factoryRecipeThread.getActiveRecipe().parallelism
                                val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                                    world,
                                    (chunkCoord(x) + i),
                                    (chunkCoord(z) + ii)
                                )
                                val mineral: MineralMix? = getUsableMix(worldInfo)
                                if (mineral != null) {
                                    data.setString("kmm$k$kk", mineral.name)
                                } else {
                                    data.setString("kmm$k$kk", "empty")
                                }
                                data.setInteger("sfsh", 6000)
                                data.setInteger("bxs$k$kk", bxs)
                            }
                            .addInput(dust).setChance(0.05f)
                        for (j in 0..2) {
                            r1.addOutput(stone)
                                .addItemModifier { ctrl, item ->
                                    if (this.isDimensional()) {
                                        val poss = ctrl.controller.customDataTag.getIntArray("pos")
                                        val pos = BlockPos(poss[0], poss[1], poss[2])
                                        return@addItemModifier getOreOutput(ctrl.controller, pos, poss[3])
                                    } else {
                                        return@addItemModifier getOreOutput(
                                            ctrl.controller,
                                            ctrl.controller.getPos(),
                                            ctrl.iWorld.dimension
                                        )
                                    }
                                }
                        }

                        r1.requireComputationPoint(3f)
                            .addOutput(stone)
                            .addItemModifier { ctrl, item ->
                                getCcrystalOutput(ctrl.controller)
                            }
                            .setChance(0.04f)
                            .setMaxThreads(1)
                            .setParallelized(true)
                            .setThreadName(threadName)
                            .build()
                        thread.addRecipe(recipeName)
                    }
                }
            }
        }
        machine.setInternalParallelism(this.parallelism)
        HyperNetHelper.proxyMachineForHyperNet(registryName)
        if (!this.isDimensional()) {
            when (this.getType()) {
                SINGLE -> machine.addMachineEventHandler(
                    MachineStructureFormedEvent::class.java
                ) { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val x = ctrl.getPos().x
                    val z = ctrl.getPos().z
                    val world = ctrl.getWorld()
                    val kmm = data.getString("kmm11")
                    val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                        world,
                        chunkCoord(x),
                        chunkCoord(z)
                    )
                    val mineral: MineralMix? = getUsableMix(worldInfo)
                    if (mineral != null) {
                        if (kmm != mineral.name) {
                            data.setString("kmm11", mineral.name)
                        }
                    } else {
                        data.setString("kmm11", "empty")
                    }
                }

                RANGE -> machine.addMachineEventHandler(
                    MachineStructureFormedEvent::class.java
                ) { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val x = ctrl.getPos().x
                    val z = ctrl.getPos().z
                    val world = ctrl.getWorld()
                    for (i in tqsz) {
                        val k = i + 1
                        for (ii in tqsz) {
                            val kk = ii + 1
                            val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                                world,
                                chunkCoord(x) + i,
                                chunkCoord(z) + ii
                            )
                            val mineral: MineralMix? = getUsableMix(worldInfo)
                            if (mineral != null) {
                                data.setString("kmm$k$kk", mineral.name)
                            } else {
                                data.setString("kmm$k$kk", "empty")
                            }
                            if (data.getString("kmm$k$kk") != "empty") {
                                data.setInteger("depletion$k$kk", worldInfo.depletion)
                            }
                        }
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private fun regGui(machine: DynamicMachine) {
        when (this.getType()) {
            SINGLE -> machine.addMachineEventHandler(
                ControllerGUIRenderEvent::class.java
            ) { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                val research_progress = data.getByte("research_progress")
                val components_amount = data.getByte("components_amount")
                val info = ObjectArrayList<String>()
                info.add(
                    I18n.format("top.drill.status") + "§6[" +
                            I18n.format("top.drill.research_progress") + research_progress + "|" +
                            I18n.format("top.drill.components_amount") + components_amount + "§6]"

                )
                if (data.hasKey("additional_component_raw_ore")) info.add(I18n.format("top.drill.components_raw_ore"))
                var kmm = data.getString("kmm11")
                val i18nkmm = I18n.format("desc.immersiveengineering.info.mineral.$kmm")
                if (!i18nkmm.startsWith("desc")) kmm = i18nkmm
                val depletion = data.getInteger("depletion11")
                if (!kmm.isEmpty() && kmm != "empty") info.add(I18n.format("novaeng.drill.mineral.name") + kmm)
                else if (kmm.isEmpty()) info.add(I18n.format("novaeng.drill.mineral.empyt.s"))
                if (!kmm.isEmpty() && kmm != "empty") info.add(I18n.format("novaeng.drill.mineral.depletion.s") + (MMMineralMix - depletion))
                if (kmm.isEmpty()) info.add(I18n.format("novaeng.drill.mineral.empyt"))
                @Suppress("UsePropertyAccessSyntax")
                event.setExtraInfo(*info.toTypedArray())
            }

            RANGE -> machine.addMachineEventHandler(
                ControllerGUIRenderEvent::class.java
            ) { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                val research_progress = data.getByte("research_progress")
                val components_amount = data.getByte("components_amount")
                val info = ObjectArrayList<String>()
                info.add(
                    I18n.format("top.drill.status") + "§6[" +
                            I18n.format("top.drill.research_progress") + research_progress + "|" +
                            I18n.format("top.drill.components_amount") + components_amount + "§6]"

                )
                if (data.hasKey("additional_component_raw_ore")) info.add(I18n.format("top.drill.components_raw_ore"))
                for (i in tqsz) {
                    val k = i + 1
                    for (ii in tqsz) {
                        val kk = ii + 1
                        var kmm = data.getString("kmm$k$kk")
                        val i18nkmm = I18n.format("desc.immersiveengineering.info.mineral.$kmm")
                        if (!i18nkmm.startsWith("desc")) kmm = i18nkmm
                        val depletion = data.getInteger("depletion$k$kk")
                        if (!kmm.isEmpty() && kmm != "empty") info.add(
                            I18n.format(
                                "novaeng.drill.mineral.depletion.r",
                                tqdzb.getInt((k.toString() + kk)),
                                kmm
                            ) + (MMMineralMix - depletion)
                        )
                        else if (kmm.isEmpty()) info.add(
                            I18n.format(
                                "novaeng.drill.mineral.empyt.r",
                                tqdzb.getInt((k.toString() + kk))
                            )
                        )
                        if (kmm == "empty") info.add(I18n.format("novaeng.drill.mineral.empyt"))
                    }
                }
                @Suppress("UsePropertyAccessSyntax")
                event.setExtraInfo(*info.toTypedArray())
            }
        }
    }

    private fun regUpgrade(machine: DynamicMachine) {
        val upThreadName = "novaeng.drill.thread.up"
        val upThread = FactoryRecipeThread.createCoreThread(upThreadName)
        val name = this.getMachineName()
        if (this.isDimensional()) {
            RecipeBuilder.newBuilder("excavatorzb$name", name, 10)
                .addInput(itemUtils.getItem("contenttweaker:zbk", 0))
                .setNBTChecker { ctrl, item ->
                    val data = ctrl.controller.customDataTag
                    if (!data.hasKey("binding")) {
                        return@setNBTChecker false
                    }
                    val pos = IDataUtils.getIntArray(item.tag, "pos", null) ?: return@setNBTChecker false
                    data.setIntArray("poss", pos)
                    true
                }
                .addOutput(itemUtils.getItem("contenttweaker:zbk", 0))
                .addPreCheckHandler { event ->
                    val ctrl = event.getController()
                    if (ctrl.isWorking) {
                        event.setFailed("novaeng.machine.failed.work")
                    }
                }
                .addFactoryStartHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val poss = data.getIntArray("poss")
                    val world = DimensionManager.getWorld(poss[3])
                    when (this.getType()) {
                        SINGLE -> {
                            val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                                world,
                                chunkCoord(poss[0]),
                                chunkCoord(poss[2])
                            )
                            val mineral: MineralMix? = getUsableMix(worldInfo)
                            if (mineral != null) {
                                data.setString("kmm11", mineral.name)
                            }
                            if (!data.getString("kmm11").isEmpty()) {
                                data.setInteger("depletion11", worldInfo.depletion)
                            }
                        }

                        RANGE -> {
                            for (i in tqsz) {
                                val k = i + 1
                                for (ii in tqsz) {
                                    val kk = ii + 1
                                    val worldInfo: MineralWorldInfo = ExcavatorHandler.getMineralWorldInfo(
                                        world,
                                        chunkCoord(poss[0]) + i,
                                        chunkCoord(poss[2]) + ii
                                    )
                                    val mineral: MineralMix? = getUsableMix(worldInfo)
                                    if (mineral != null) {
                                        data.setString("kmm$k$kk", mineral.name)
                                    }
                                    if (!data.getString("kmm$k$kk").isEmpty()) {
                                        data.setInteger("depletion$k$kk", worldInfo.depletion)
                                    }
                                }
                            }
                        }
                    }
                    data.setIntArray("pos", poss)
                }
                .setLoadJEI(false)
                .setParallelized(false)
                .setThreadName(upThreadName)
                .build()
            upThread.addRecipe("excavatorzb$name")
        }
        for (i in 0..2) {
            upThread.addRecipe("research_mineral_utilization_" + name + "_" + i)
            upThread.addRecipe("additional_component_loading_" + name + "_" + i)


            RecipeBuilder.newBuilder("research_mineral_utilization_" + name + "_" + i, name, 10)
                .addPreCheckHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    data.getByte("research_progress")
                    data.getByte("components_amount")
                    val component = data.getBoolean("research_mineral_$i")
                    if (component) {
                        event.setFailed("novaeng.machine.failed.work")
                    }
                }
                .addFactoryFinishHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val research_progress = data.getByte("research_progress")

                    ctrl.addPermanentModifier(
                        "research$i",
                        RecipeModifierBuilder.create(
                            "modularmachinery:energy",
                            "input",
                            (1 + (0.2 * (i + 1))).toFloat(),
                            1,
                            false
                        ).build()
                    )
                    data.setBoolean("research_mineral_$i", true)
                    data.setByte("research_progress", (research_progress + 1).toByte())
                }.requireResearch("research_mineral_utilization_$i")
                .setParallelized(false)
                .setThreadName(upThreadName)
                .setLoadJEI(false)
                .build()

            RecipeBuilder.newBuilder("additional_component_loading_" + name + "_" + i, name, 100, 1)
                .addItemInput(itemUtils.getItem("contenttweaker:additional_component_$i", 0))
                .addPreCheckHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    data.getByte("research_progress")
                    data.getByte("components_amount")
                    val component = data.getBoolean("additional_component_$i")
                    if (component) {
                        event.setFailed("novaeng.machine.failed.work")
                    }
                }
                .addFactoryFinishHandler { event ->
                    val ctrl = event.getController()
                    val data = ctrl.customDataTag
                    val components_amount = data.getByte("components_amount")

                    ctrl.addPermanentModifier(
                        "additional$i",
                        RecipeModifierBuilder.create(
                            "modularmachinery:energy",
                            "input",
                            (1 + (0.3 * (i + 1))).toFloat(),
                            1,
                            false
                        ).build()
                    )
                    ctrl.addPermanentModifier(
                        "additionalout",
                        RecipeModifierBuilder.create(
                            "modularmachinery:item",
                            "output",
                            ((components_amount + 1).toDouble().pow(3.0) * 2).toFloat(),
                            1,
                            false
                        ).build()
                    )

                    data.setBoolean("additional_component_$i", true)
                    data.setByte("additional_component_$i", (components_amount + 1).toByte())
                }.requireResearch("additional_component_loading_$i")
                .setThreadName(upThreadName)
                .setParallelized(false)
                .setLoadJEI(false)
                .build()
        }
        upThread.addRecipe("additional_component_loading_" + name + "_3")
        RecipeBuilder.newBuilder("additional_component_loading_" + name + "_3", name, 100, 1)
            .addItemInput(itemUtils.getItem("contenttweaker:additional_component_3", 0))
            .addPreCheckHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                data.getByte("research_progress")
                data.getByte("components_amount")
                val additional_component_3 = data.getBoolean("additional_component_3")
                if (additional_component_3) {
                    event.setFailed("novaeng.machine.failed.work")
                }
            }
            .addFactoryFinishHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                val components_amount = data.getByte("components_amount")
                val research_progress = data.getByte("research_progress")
                ctrl.addPermanentModifier(
                    "additional_ex",
                    RecipeModifierBuilder.create("modularmachinery:energy", "input", 4f, 1, false).build()
                )
                ctrl.addPermanentModifier(
                    "additionalout",
                    RecipeModifierBuilder.create(
                        "modularmachinery:item",
                        "output",
                        ((components_amount + 1).toDouble().pow(3.0) * 2).toFloat(),
                        1,
                        false
                    ).build()
                )
                data.setBoolean("additional_component_3", true)
                data.setByte("components_amount", (components_amount + 1).toByte())
                data.setByte("research_progress", (research_progress + 1).toByte())
            }
            .setThreadName(upThreadName)
            .setParallelized(false)
            .setLoadJEI(false)
            .requireResearch("additional_component_loading_ex")
            .build()
        upThread.addRecipe("additional_component_loading_" + name + "_raw_ore")

        RecipeBuilder.newBuilder("additional_component_loading_" + name + "_raw_ore", name, 100, 1)
            .addItemInput(itemUtils.getItem("contenttweaker:additional_component_raw_ore", 0))
            .addPreCheckHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                data.getByte("research_progress")
                data.getByte("components_amount")
                val additional_component_raw_ore = data.getBoolean("additional_component_raw_ore")
                if (additional_component_raw_ore) {
                    event.setFailed("novaeng.machine.failed.work")
                }
            }
            .addFactoryFinishHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                data.setBoolean("additional_component_raw_ore", true)
                ctrl.addPermanentModifier(
                    "additional_raw_ore",
                    RecipeModifierBuilder.create("modularmachinery:energy", "input", 2f, 1, false).build()
                )
                ctrl.customDataTag = data
            }.requireResearch("additional_component_loading_raw_ore")
            .setThreadName(upThreadName)
            .setParallelized(false)
            .setLoadJEI(false)
            .build()
        machine.addCoreThread(upThread)
        machine.setMaxThreads(0)
    }

    override fun onTOPInfo(
        probeMode: ProbeMode,
        probeInfo: IProbeInfo,
        player: EntityPlayer,
        ipData: IProbeHitData,
        controller: TileMultiblockMachineController
    ) {
        val data = controller.customDataTag
        val research_progress = data.getByte("research_progress")
        val components_amount = data.getByte("components_amount")
        MachineSpecial.newBox(probeInfo)
            .text("{*top.drill.status*}  ")
            .text("{*top.drill.research_progress*}$research_progress  ")
            .text("{*top.drill.components_amount*}$components_amount")
    }

    protected enum class Type {
        SINGLE,
        RANGE
    }

}