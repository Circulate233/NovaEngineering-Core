package github.kasuminova.novaeng.common.machine

import WayofTime.bloodmagic.altar.BloodAltar
import WayofTime.bloodmagic.api.impl.BloodMagicAPI
import WayofTime.bloodmagic.block.BlockLifeEssence
import WayofTime.bloodmagic.core.RegistrarBloodMagicBlocks
import WayofTime.bloodmagic.core.data.Binding
import WayofTime.bloodmagic.ritual.IMasterRitualStone
import WayofTime.bloodmagic.ritual.types.RitualWellOfSuffering
import WayofTime.bloodmagic.tile.TileAltar
import WayofTime.bloodmagic.util.helper.NetworkHelper
import crafttweaker.CraftTweakerAPI.itemUtils
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.minecraft.CraftTweakerMC
import crafttweaker.api.oredict.IOreDictEntry
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent
import github.kasuminova.mmce.common.event.machine.MachineTickEvent
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.util.Functions.asList
import github.kasuminova.novaeng.common.util.Functions.getText
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor
import ink.ikx.mmce.common.utils.StackUtils
import it.unimi.dsi.fastutil.objects.ObjectLists
import kport.modularmagic.common.tile.TileLifeEssenceProvider
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@ZenRegister
@ZenClass("novaeng.MMAltar")
object MMAltar : MachineSpecial {

    private val chace: MutableMap<TileMultiblockMachineController, TileAltar> = WeakHashMap()
    val pdfw1 = intArrayOf(-6, -10, -6)
    val pdfw2 = intArrayOf(6, 10, 6)

    const val MACHINEID = "mm_altar"
    val REGISTRY_NAME = ResourceLocation(ModularMachinery.MODID, MACHINEID)

    val posSet1: List<BlockPos> = asList(
        BlockPos(-3, -5, -3), BlockPos(-3, -5, 3),
        BlockPos(3, -5, -3), BlockPos(3, -5, 3),
        BlockPos(-3, -4, -3), BlockPos(-3, -4, 3),
        BlockPos(3, -4, -3), BlockPos(3, -4, 3)
    )

    val posSet2: List<BlockPos> = asList(
        BlockPos(-5, -3, -5), BlockPos(-5, -3, 5),
        BlockPos(5, -3, -5), BlockPos(5, -3, 5)
    )

    val posSet3: List<BlockPos> = asList(
        BlockPos(-11, -2, -11), BlockPos(-11, -2, 11),
        BlockPos(11, -2, -11), BlockPos(11, -2, 11)
    )

    //水晶矩阵
    var BLOCKSJ1: Block = getOtherModsBlock("contenttweaker", "crystalmatrixforcefieldcontrolblock")

    //落星合金
    var BLOCKSJ2: Block = getOtherModsBlock("contenttweaker", "fallenstarforcefieldcontrolblock")

    //寰宇
    var BLOCKSJ3: Block = getOtherModsBlock("contenttweaker", "universalforcefieldcontrolblock")

    private fun getOtherModsBlock(modId: String, blockName: String): Block {
        return GameRegistry.findRegistry(Block::class.java)
            .getValue(ResourceLocation(modId, blockName)) ?: Blocks.AIR
    }

    private fun buildModifierReplacementBlockArray(block: Block, posSet: List<BlockPos>): BlockArray {
        val blockArray = BlockArray()
        val descriptor = IBlockStateDescriptor(block)
        posSet.forEach { pos ->
            blockArray.addBlock(
                pos, BlockArray.BlockInformation(
                    mutableListOf(descriptor)
                )
            )
        }
        return blockArray
    }

    override fun preInit(machine: DynamicMachine) {
        machine.addMachineEventHandler(
            MachineStructureUpdateEvent::class.java
        ) {
            val controller = it.getController()
            controller.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC)
        }
        machine.addMachineEventHandler(MachineStructureUpdateEvent::class.java) {
            val ctrl = it.controller
            val nbt = ctrl.customDataTag
            val altar = ctrl.getAltar()

            var sacrifice: Short = 0
            for (pos in ctrl.foundPattern.getPattern().keys) {
                val realPos: BlockPos = ctrl.getPos().add(pos.x, pos.y, pos.z)
                val state: IBlockState = ctrl.getWorld().getBlockState(realPos)
                if (state.block == RegistrarBloodMagicBlocks.BLOOD_RUNE) {
                    if (state.block.getMetaFromState(state) == 3) ++sacrifice
                }
            }
            var level = altar.getLevel().toByte()
            var upgrade: Byte = 0

            ctrl.foundMachine?.let { it1 ->
                for (replacement in it1.multiBlockModifiers) {
                    if (replacement.matches(ctrl)) {
                        when (replacement.modifierName) {
                            "upgrade1" -> ++upgrade
                            "upgrade2" -> ++upgrade
                            "upgrade3" -> {
                                upgrade = (2.toByte() + upgrade).toByte()
                                ++level
                            }
                        }
                    }
                }
            }

            nbt.setShort("sacrifice", sacrifice)
            nbt.setByte("level", level)
            nbt.setByte("upgrade", upgrade)
        }
        machine.addMachineEventHandler(MachineTickEvent::class.java) {
            val ctrl = it.controller

            if (ctrl.world.totalWorldTime % 100 != 0.toLong()) return@addMachineEventHandler

            val nbt = ctrl.customDataTag
            val altar = ctrl.getAltar()

            var level = altar.getLevel().toByte()
            var upgrade: Byte = 0

            ctrl.foundMachine?.let { it1 ->
                for (replacement in it1.multiBlockModifiers) {
                    if (replacement.matches(ctrl)) {
                        when (replacement.modifierName) {
                            "upgrade1" -> ++upgrade
                            "upgrade2" -> ++upgrade
                            "upgrade3" -> {
                                upgrade = (2.toByte() + upgrade).toByte()
                                ++level
                            }
                        }
                    }
                }
            }

            nbt.setByte("level", level)
            nbt.setByte("upgrade", upgrade)
        }
        machine.multiBlockModifiers.add(
            MultiBlockModifierReplacement(
                "upgrade1",
                buildModifierReplacementBlockArray(BLOCKSJ1, posSet1),
                ObjectLists.emptyList(),
                asList(
                    getText("novaeng.mm_altar.upgrade.0"),
                    getText("novaeng.mm_altar.upgrade1") + BLOCKSJ1.localizedName,
                    getText("novaeng.mm_altar.upgrade.1", 1)
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ1.defaultState)
            )
        )
        machine.multiBlockModifiers.add(
            MultiBlockModifierReplacement(
                "upgrade2",
                buildModifierReplacementBlockArray(BLOCKSJ2, posSet2),
                ObjectLists.emptyList(),
                asList(
                    getText("novaeng.mm_altar.upgrade.0"),
                    getText("novaeng.mm_altar.upgrade2") + BLOCKSJ2.localizedName,
                    getText("novaeng.mm_altar.upgrade.1", 1)
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ2.defaultState)
            )
        )
        machine.multiBlockModifiers.add(
            MultiBlockModifierReplacement(
                "upgrade3",
                buildModifierReplacementBlockArray(BLOCKSJ3, posSet3),
                ObjectLists.emptyList(),
                asList(
                    getText("novaeng.mm_altar.upgrade.0"),
                    getText("novaeng.mm_altar.upgrade3.0") + BLOCKSJ3.localizedName,
                    getText("novaeng.mm_altar.upgrade.1", 2),
                    getText("novaeng.mm_altar.upgrade3.1")
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ3.defaultState)
            )
        )
        val altarRecipe = BloodMagicAPI.INSTANCE.recipeRegistrar
        altarRecipe.removeBloodAltar(ItemStack(Items.BUCKET))
        for (recipe in altarRecipe.altarRecipes) {
            registerRecipe(
                recipe.consumeRate,
                recipe.syphon,
                recipe.minimumTier.toInt(),
                CraftTweakerMC.getIIngredient(recipe.input),
                CraftTweakerMC.getIItemStack(recipe.output)
            )
        }
        RecipeBuilder.newBuilder("czshr", MACHINEID, 10)
            .addItemInput(itemUtils.getItem("bloodmagic:sacrificial_dagger", 1)).setChance(0.toFloat())
            .addFactoryFinishHandler { it.controller.getAltar().addBlood(Int.MAX_VALUE) }
            .addRecipeTooltip("novaeng.mm_altar.recipe.8")
            .setParallelized(false)
            .setThreadName("novaeng.mm_altar.thread.1")
            .build()
        RecipeBuilder.newBuilder("lhwl", MACHINEID, 10)
            .addPreCheckHandler {
                val ctrl = it.controller
                val altar = ctrl.getAltar()
                val now = altar.getNowBlood()
                if (now < 10000) {
                    it.setFailed("novaeng.mm_altar.failed.0")
                    return@addPreCheckHandler
                }
                val pos = BlockPos.PooledMutableBlockPos.retain(
                    ctrl.pos.x, ctrl.pos.y - 1, ctrl.pos.z
                )
                val t = ctrl.world.getTileEntity(pos)
                pos.release()
                if (t is TileLifeEssenceProvider.Output) {
                    val item = t.inventory.getStackInSlot(0)
                    if (item.isEmpty) {
                        it.setFailed("novaeng.mm_altar.failed.1")
                        return@addPreCheckHandler
                    }
                    val soulNetwork = NetworkHelper.getSoulNetwork(Binding.fromStack(item))
                    if (soulNetwork.currentEssence >= NetworkHelper.getMaximumForTier(
                            NetworkHelper.getCurrentMaxOrb(
                                soulNetwork
                            )
                        )
                    ) {
                        it.setFailed("novaeng.mm_altar.failed.2")
                    }
                } else it.setFailed("novaeng.mm_altar.failed.1")
            }
            .addFactoryFinishHandler {
                val ctrl = it.controller
                val altar = ctrl.getAltar()
                val pos = BlockPos.PooledMutableBlockPos.retain(
                    ctrl.pos.x, ctrl.pos.y - 1, ctrl.pos.z
                )
                val t = ctrl.world.getTileEntity(pos)
                pos.release()
                if (t is TileLifeEssenceProvider.Output) {
                    val item = t.inventory.getStackInSlot(0)
                    val Soul = NetworkHelper.getSoulNetwork(Binding.fromStack(item))
                    Soul.currentEssence += altar.drainBlood(10000)
                }

            }
            .addRecipeTooltip(
                "novaeng.mm_altar.recipe.6",
                "novaeng.mm_altar.recipe.7"
            )
            .setParallelized(false)
            .setThreadName("novaeng.mm_altar.thread.2")
            .build()
        RecipeBuilder.newBuilder("mm_altar_level_infinity_orb", "mm_altar", 2147483647, 9999999, false)
            .addItemInputs(itemUtils.getItem("contenttweaker:universalforcefieldcontrolblock", 0).amount(4))
            .addPreCheckHandler {
                val ctrl = it.controller
                val data = ctrl.customDataTag
                val xycc = ctrl.getAltar().getNowBlood()
                val jtdj = data.getByte("level")

                if (xycc < 2147483647) {
                    it.setFailed("novaeng.mm_altar.failed.blood")
                    return@addPreCheckHandler
                }
                if (jtdj < 7) {
                    it.setFailed("novaeng.mm_altar.failed.level")
                    return@addPreCheckHandler
                }
            }
            .addFactoryStartHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag

                data.setLong("progress", 0)
                data.setLong("totalProgress", 214748364700L)
            }
            .addFactoryPreTickHandler {
                val ctrl = it.controller
                val data = ctrl.customDataTag
                val altar = ctrl.getAltar()
                val now = altar.getNowBlood()
                val progress = data.getLong("progress")
                val thread = it.factoryRecipeThread
                val totalTick = thread.activeRecipe.totalTick

                if (progress < 214748364700L) {
                    it.preventProgressing(
                        getText(
                            "novaeng.mm_altar.progress.0",
                            214748364700L - progress
                        )
                    )
                }
                if (now == 2147483647) {
                    if ((progress + 2147483647) <= 214748364700L) {
                        data.setLong("progress", progress + 2147483647)
                        altar.setBlood(0)
                    } else {
                        data.setLong("progress", 214748364700L)
                        altar.setBlood(0)
                        thread.activeRecipe.tick = totalTick
                    }
                }
            }
            .addOutput(itemUtils.getItem("contenttweaker:level_infinity_orb", 0))
            .setThreadName("novaeng.mm_altar.thread.0")
            .addRecipeTooltip(
                getText("novaeng.mm_altar.recipe.0", 2147483647),
                getText("novaeng.mm_altar.recipe.1", 214748364700L),
                getText("novaeng.mm_altar.recipe.2", 7),
                "novaeng.mm_altar.recipe.5"
            )
            .setParallelized(false)
            .build()
        if (NovaEngineeringCore.proxy.isClient) clientInit(machine)
    }

    @SideOnly(Side.CLIENT)
    private fun clientInit(machine: DynamicMachine) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent::class.java) {
            val ctrl = it.controller
            val altar = ctrl.getAltar()
            val nbt = ctrl.customDataTag

            val level = nbt.getByte("level")
            val upgrade = nbt.getByte("upgrade")
            val now = altar.getNowBlood()
            val max = altar.getMaxBlood()

            var check = nbt.hasKey("pos")
            if (check) {
                val p = nbt.getIntArray("pos")
                val pos = BlockPos.PooledMutableBlockPos.retain(
                    p[0], p[1], p[2]
                )
                val t = ctrl.world.getTileEntity(pos)
                pos.release()
                if (!(t is IMasterRitualStone && t.currentRitual is RitualWellOfSuffering)) {
                    nbt.removeTag("pos")
                    check = ergodicPos(ctrl, ctrl.pos) { x, y, z ->
                        nbt.setIntArray("pos", intArrayOf(x, y, z))
                    }
                }
            } else {
                check = ergodicPos(ctrl, ctrl.pos) { x, y, z ->
                    nbt.setIntArray("pos", intArrayOf(x, y, z))
                }
            }

            val info = asList(
                getText("gui.mm_altar.tooltip.0", now, max),
                getText("gui.mm_altar.tooltip.1", level, upgrade),
                getText("gui.mm_altar.tooltip.2") + getText("mmaltar.well_of_suffering.$check")
            )
            if (!check) {
                info.add(getText("gui.mm_altar.tooltip.3"))
                info.add(getText("gui.mm_altar.tooltip.4"))
                info.add(getText("gui.mm_altar.tooltip.5"))
            }
            @Suppress("UsePropertyAccessSyntax")
            it.setExtraInfo(*info.toTypedArray())
        }
    }

    override fun getRegistryName(): ResourceLocation {
        return REGISTRY_NAME
    }

    /**
     * 合成配方
     *
     * @param need      最低血液需求
     * @param maxNeed   总血液需求
     * @param altarTier 祭坛等级需求
     * @param input     输入物品
     * @param output    输出物品
     */
    @ZenMethod
    @JvmStatic
    fun registerRecipe(need: Int, maxNeed: Int, altarTier: Int, input: IIngredient, output: IItemStack) {
        val time = maxNeed / need
        val name: String = when (input) {
            is IItemStack -> CraftTweakerMC.getItem(input.definition).getRegistryName().toString() + input.metadata
            is IOreDictEntry -> input.name
            else -> CraftTweakerMC.getItem(output.definition).getRegistryName().toString() + output.metadata
        }
        RecipeBuilder.newBuilder(name, MACHINEID, time, 1000)
            .addItemInputs(input)
            .addPreCheckHandler { event ->
                val ctrl = event.getController()
                val nbt = ctrl.customDataTag
                val altar = ctrl.getAltar()
                val now = altar.getNowBlood()
                val max = altar.getMaxBlood()
                val level = nbt.getByte("level")
                val upgrade = nbt.getByte("upgrade")
                val bx = min(4.0.pow(upgrade.toDouble()), (max.toDouble() / need))

                if (now < need) {
                    event.setFailed("novaeng.mm_altar.failed.blood")
                    return@addPreCheckHandler
                }
                if (level < altarTier) {
                    event.setFailed("novaeng.mm_altar.failed.level")
                    return@addPreCheckHandler
                }
                event.activeRecipe.maxParallelism = bx.toInt()
            }
            .addFactoryStartHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                val bx = event.factoryRecipeThread.getActiveRecipe().parallelism

                data.setLong("progress", 0)
                data.setLong("totalProgress", bx.toLong() * maxNeed)
            }
            .addFactoryPreTickHandler { event ->
                val ctrl = event.getController()
                val data = ctrl.customDataTag
                val altar = ctrl.getAltar()
                val nowBlood = altar.getNowBlood()
                val progress = data.getLong("progress")
                val totalProgress = data.getLong("totalProgress")
                val consumption = altar?.consumptionMultiplier ?: 0f
                val thread = event.factoryRecipeThread
                val bx = thread.getActiveRecipe().parallelism
                val totalTick = thread.getActiveRecipe().totalTick
                val sjneed = need * bx
                if (nowBlood > sjneed) {
                    if (progress < totalProgress) {
                        thread.getActiveRecipe().setTick((totalTick / 2))
                        event.preventProgressing(
                            getText(
                                "novaeng.mm_altar.progress.0",
                                totalProgress - progress
                            )
                        )
                    }
                    if (nowBlood <= (consumption + 1.0f) * sjneed) {
                        if (progress + nowBlood <= totalProgress) {
                            data.setLong("progress", progress + nowBlood)
                            altar.setBlood(0)
                        } else {
                            data.setLong("progress", totalProgress)
                            altar.drainBlood((totalProgress - progress).toInt())
                            thread.getActiveRecipe().tick = totalTick
                        }
                    } else {
                        if (progress + ((1 + consumption) * sjneed) <= totalProgress) {
                            data.setLong("progress", (progress + ((1 + consumption) * sjneed)).toLong())
                            altar.drainBlood(((1.0f + consumption) * sjneed).toInt())
                        } else {
                            data.setLong("progress", totalProgress)
                            altar.drainBlood((totalProgress - progress).toInt())
                            thread.getActiveRecipe().tick = totalTick
                        }
                    }
                } else {
                    event.preventProgressing(
                        getText(
                            "novaeng.mm_altar.progress.1",
                            sjneed
                        )
                    )
                }
            }
            .addOutput(output)
            .setThreadName("novaeng.mm_altar.thread.0")
            .addRecipeTooltip(
                getText("novaeng.mm_altar.recipe.0", need),
                getText("novaeng.mm_altar.recipe.1", maxNeed),
                getText("novaeng.mm_altar.recipe.2", altarTier),
                "novaeng.mm_altar.recipe.3",
                "novaeng.mm_altar.recipe.4"
            )
            .build()
    }

    fun TileMultiblockMachineController.getAltar(): TileAltar? {
        var t = chace[this]
        if (t == null || t.isInvalid) {
            t = this.world.getTileEntity(this.pos.offset(EnumFacing.DOWN, 4)) as? TileAltar
        } else return t
        if (t != null) {
            synchronized(MMAltar) {
                chace.put(this, t)
            }
        }
        return t
    }

    private fun TileAltar?.getLevel(): Int {
        if (this == null) return 0
        return this.tier.ordinal + 1
    }

    private fun TileAltar?.getNowBlood(): Int {
        return this?.getBloodAltar()?.fluid?.amount ?: 0
    }

    fun TileAltar?.setBlood(value: Int) {
        val altar = this?.getBloodAltar() ?: return
        if (altar.fluid == null) {
            altar.setMainFluid(FluidStack(BlockLifeEssence.getLifeEssence(), value))
            return
        }
        altar.fluid.amount = value
    }

    fun TileAltar?.addBlood(value: Int) {
        val altar = this?.getBloodAltar() ?: return
        if (altar.fluid == null) {
            altar.setMainFluid(FluidStack(BlockLifeEssence.getLifeEssence(), value))
            return
        }
        val fluid = altar.fluid
        val max = altar.capacity
        val need = max - fluid.amount
        fluid.amount = if (value >= need) max else fluid.amount + value
    }

    private fun TileAltar?.drainBlood(value: Int): Int {
        val altar = this?.getBloodAltar() ?: return 0
        val fluid = altar.fluid ?: return 0
        val o = fluid.amount
        fluid.amount = max(0, fluid.amount - value)
        return o - fluid.amount
    }

    private fun TileAltar.getBloodAltar(): BloodAltar? {
        return this.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null) as? BloodAltar
    }

    private fun TileAltar?.getMaxBlood(): Int {
        return this?.capacity ?: 0
    }

    fun clamp(a: Int, min: Int, max: Int): Int {
        return min(max(a, max), max)
    }

    inline fun ergodicPos(
        ctrl: TileMultiblockMachineController,
        pos: BlockPos,
        run: (x: Int, y: Int, z: Int) -> Unit
    ): Boolean {
        val p = BlockPos.MutableBlockPos()
        for (x in (pos.x + pdfw1[0])..(pos.x + pdfw2[0] + 1)) {
            for (y in clamp((pos.y + pdfw1[1]), 0, 255)..(clamp((pos.y + pdfw2[1]), 0, 255))) {
                for (z in (pos.z + pdfw1[2])..(pos.z + pdfw2[2] + 1)) {
                    val tile = ctrl.world.getTileEntity(p.setPos(x, y, z))
                    if (tile is IMasterRitualStone) {
                        if (tile.currentRitual is RitualWellOfSuffering) {
                            run(x, y, z)
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}