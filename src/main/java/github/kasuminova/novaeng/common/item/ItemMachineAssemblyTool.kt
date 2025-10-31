@file:Suppress("DEPRECATION")

package github.kasuminova.novaeng.common.item

import appeng.api.networking.crafting.ICraftingGrid
import appeng.util.item.AEItemStack
import com.brandon3055.draconicevolution.api.itemconfig.BooleanConfigField
import com.brandon3055.draconicevolution.api.itemconfig.IConfigurableItem
import com.brandon3055.draconicevolution.api.itemconfig.IItemConfigField
import com.brandon3055.draconicevolution.api.itemconfig.IntegerConfigField
import com.brandon3055.draconicevolution.api.itemconfig.ItemConfigFieldRegistry
import com.brandon3055.draconicevolution.api.itemconfig.ToolConfigHelper
import com.circulation.random_complement.common.interfaces.RCCraftingGridCache
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.CommonProxy
import github.kasuminova.novaeng.common.util.AssemblyBlockArray
import github.kasuminova.novaeng.common.util.AutoCraftingQueue
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.machine.MachineRegistry
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import hellfirepvp.modularmachinery.common.util.BlockArrayCache
import ink.ikx.mmce.common.utils.StructureIngredient
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.util.text.translation.I18n
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

object ItemMachineAssemblyTool : ItemBasic("machine_assembly_tool"), IConfigurableItem {

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack> {
        if (hand != EnumHand.MAIN_HAND || player.isSneaking) return super.onItemRightClick(world, player, hand)
        if (world.isRemote) {
            player.openGui(
                NovaEngineeringCore.instance,
                CommonProxy.GuiType.MACHINE_ASSEMBLY_TOOL.ordinal,
                world,
                0,
                0,
                0
            )
        }
        return ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand))
    }

    override fun onItemUse(
        player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): EnumActionResult {
        if (hand != EnumHand.MAIN_HAND || world.isRemote) return EnumActionResult.PASS
        val tile = world.getTileEntity(pos)
        if (tile == null) {
            return EnumActionResult.PASS
        } else if (player.isSneaking) {
            if (!NEWMachineAssemblyManager.checkMachineAssembly(player)) {
                var state = world.getBlockState(pos)
                val block = state.block
                @Suppress("DEPRECATION")
                state = block.getActualState(state, world, pos)
                val machine: DynamicMachine
                val controllerFacing: EnumFacing
                if (tile is TileMultiblockMachineController) {
                    machine = tile.blueprintMachine ?: if (block is BlockController)
                        block.getParentMachine()
                    else return EnumActionResult.FAIL
                    controllerFacing = player.world.getBlockState(pos).getValue(BlockController.FACING)
                } else {
                    val meta = block.getMetaFromState(state)
                    var m: DynamicMachine? = null
                    var e: EnumFacing? = null
                    for (entry in NEWMachineAssemblyManager.getConstructorsIterator()) {
                        if (entry.key == block && entry.value.containsKey(meta)) {
                            m = entry.value[meta]
                            val pf = block.blockState.getProperty("facing")
                            e = if (pf != null) {
                                state.getValue(pf) as? EnumFacing ?: facing
                            } else facing
                            break
                        }
                    }
                    e = if (e == EnumFacing.UP || e == EnumFacing.DOWN) {
                        EnumFacing.NORTH
                    } else e
                    machine = m ?: return EnumActionResult.FAIL
                    controllerFacing = e ?: return EnumActionResult.FAIL
                }

                val stack = player.getHeldItem(hand)
                var array = AssemblyBlockArray(
                    BlockArrayCache.getBlockArrayCache(
                        machine.pattern,
                        controllerFacing
                    )
                )

                var dynamicPatternSize = getDynamicPatternSize(stack)
                val dynamicPatterns = machine.dynamicPatterns

                for (pattern in dynamicPatterns.values) {
                    dynamicPatternSize = max(dynamicPatternSize, pattern.minSize)
                }

                for (pattern in dynamicPatterns.values) {
                    pattern.addPatternToBlockArray(
                        array,
                        min(max(pattern.minSize, dynamicPatternSize), pattern.maxSize),
                        pattern.faces.iterator().next(),
                        controllerFacing
                    )
                }

                val st = StructureIngredient.of(player.world, pos, array.copy())
                array = array.offset(pos)
                if (array.min.y < 0 || array.max.y > 255) {
                    player.sendMessage(
                        TextComponentTranslation(
                            "message.assembly.tip.too_high",
                            if (array.min.y < 1) {
                                "y = ${array.min.y}"
                            } else {
                                "y = ${array.max.y}"
                            }
                        )
                    )
                    return EnumActionResult.FAIL
                }

                val usingAE = isUsingAE(stack)
                val autoAECrafting = usingAE && isAutoAECrafting(stack)
                val missing = NEWMachineAssemblyManager.checkAllItems(player, st, usingAE, autoAECrafting)
                val q = missing.list
                if (autoAECrafting && !q.isEmpty()) {
                    MEHandler.getTerminalGuiObject(player)?.actionableNode?.grid?.let {
                        val autoList = ArrayDeque<ItemStack>()
                        val cgc: RCCraftingGridCache = it.getCache(ICraftingGrid::class.java)
                        val list = cgc.`rc$getCraftableItems`()
                        for (stacks in q) {
                            for (item in stacks) {
                                if (item.isEmpty) continue
                                if (list.containsKey(AEItemStack.fromItemStack(item))) {
                                    autoList.add(item)
                                    break
                                }
                            }
                        }
                        AutoCraftingQueue.setQueueAndStrat(autoList, player)
                    }
                }
                if (q.isEmpty()
                    || !isNeedAllIngredient(stack)
                ) {
                    array = NEWMachineAssemblyManager.addAssemblyMachine(player, array)
                    array.usingAE = usingAE
                    array.ignoreFluids = isIgnoreFluids(stack)
                    array.missing = missing.miss
                    array.start()
                    player.sendMessage(
                        TextComponentTranslation(
                            "message.assembly.tip.already_assembly.start"
                        )
                    )
                    return EnumActionResult.SUCCESS
                }
            } else {
                player.sendMessage(
                    TextComponentTranslation(
                        "message.assembly.tip.already_assembly"
                    )
                )
                return EnumActionResult.FAIL
            }
        }
        return EnumActionResult.PASS
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, world: World?, lines: MutableList<String?>, flagIn: ITooltipFlag) {
        super.addInformation(stack, world, lines, flagIn)
        lines.add("item.novaeng_core.machine_assembly_tool.config".i18n())
        lines.add(" ${usingAEConfig.unlocalizedName.i18n()} : ${isUsingAE(stack)}")
        lines.add(" ${ignoreFluidsConfig.unlocalizedName.i18n()} : ${isIgnoreFluids(stack)}")
        lines.add(" ${dynamicPatternSizeConfig.unlocalizedName.i18n()} : ${getDynamicPatternSize(stack)}")
        lines.add(" ${autoAECraftingConfig.unlocalizedName.i18n()} : ${isAutoAECrafting(stack)}")
        lines.add(" ${needAllIngredientConfig.unlocalizedName.i18n()} : ${isNeedAllIngredient(stack)}")
    }

    fun isUsingAE(stack: ItemStack): Boolean {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("UsingAE")
    }

    fun isIgnoreFluids(stack: ItemStack): Boolean {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("IgnoreFluids")
    }

    fun getDynamicPatternSize(stack: ItemStack): Int {
        return ToolConfigHelper.getFieldStorage(stack).getInteger("DynamicPatternSize")
    }

    fun isAutoAECrafting(stack: ItemStack): Boolean {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("AutoAECrafting")
    }

    fun isNeedAllIngredient(stack: ItemStack): Boolean {
        return ToolConfigHelper.getFieldStorage(stack).getBoolean("NeedAllIngredient")
    }

    fun setUsingAE(stack: ItemStack, b: Boolean) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("UsingAE", b)
    }

    fun setIgnoreFluids(stack: ItemStack, b: Boolean) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("IgnoreFluids", b)
    }

    fun setDynamicPatternSize(stack: ItemStack, size: Int) {
        ToolConfigHelper.getFieldStorage(stack).setByte("DynamicPatternSize", size.toByte())
    }

    fun setAutoAECrafting(stack: ItemStack, auto: Boolean) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("AutoAECrafting", auto)
    }

    fun setNeedAllIngredient(stack: ItemStack, auto: Boolean) {
        ToolConfigHelper.getFieldStorage(stack).setBoolean("NeedAllIngredient", auto)
    }

    val usingAEConfig by lazy {
        BooleanConfigField(
            "UsingAE",
            false,
            "UsingAE".getDescription()
        )
    }

    val ignoreFluidsConfig by lazy {
        BooleanConfigField(
            "IgnoreFluids",
            false,
            "IgnoreFluids".getDescription()
        )
    }

    val dynamicPatternSizeConfig by lazy {
        IntegerConfigField(
            "DynamicPatternSize",
            0,
            0,
            getMaxDynamicPatternSize(),
            "DynamicPatternSize".getDescription(),
            IItemConfigField.EnumControlType.SELECTIONS
        )
    }

    val autoAECraftingConfig by lazy {
        BooleanConfigField(
            "AutoAECrafting",
            false,
            "AutoAECrafting".getDescription()
        )
    }

    val needAllIngredientConfig by lazy {
        BooleanConfigField(
            "NeedAllIngredient",
            false,
            "NeedAllIngredient".getDescription()
        )
    }

    override fun getFields(
        stack: ItemStack,
        registry: ItemConfigFieldRegistry
    ): ItemConfigFieldRegistry {
        registry.register(stack, usingAEConfig)
        registry.register(stack, ignoreFluidsConfig)
        registry.register(stack, dynamicPatternSizeConfig)
        registry.register(stack, autoAECraftingConfig)
        registry.register(stack, needAllIngredientConfig)

        return registry
    }

    override fun onFieldChanged(stack: ItemStack, field: IItemConfigField) {

    }

    override fun getProfileCount(stack: ItemStack): Int {
        return 5
    }

    private fun String.getDescription(): String {
        return "text.machine_assembly_tool.config.$this".i18n()
    }

    private fun String.i18n(): String {
        return I18n.translateToLocal(this)
    }

    private fun String.i18n(vararg objs: Any): String {
        return I18n.translateToLocalFormatted(this, objs)
    }

    private fun getMaxDynamicPatternSize(): Int {
        val list = ObjectArrayList(MachineRegistry.getLoadedMachines())
        list.addAll(NEWMachineAssemblyManager.getAllDynamicMachines())

        return list.parallelStream()
            .mapToInt {
                it.dynamicPatterns.values.stream()
                    .mapToInt { it1 -> it1.maxSize }
                    .max()
                    .orElse(1)
            }
            .max()
            .orElse(1)
    }

}