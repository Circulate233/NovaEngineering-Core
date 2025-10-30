package github.kasuminova.novaeng.common.item

import appeng.api.networking.crafting.ICraftingGrid
import appeng.util.Platform.openNbtData
import appeng.util.item.AEItemStack
import com.circulation.random_complement.common.interfaces.RCCraftingGridCache
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.novaeng.common.util.AssemblyBlockArray
import github.kasuminova.novaeng.common.util.AutoCraftingQueue
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import hellfirepvp.modularmachinery.common.util.BlockArrayCache
import ink.ikx.mmce.common.utils.StructureIngredient
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

object ItemMachineAssemblyTool : ItemBasic("machine_assembly_tool") {

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack> {
        //TODO:手持打开配置GUI
        if (world.isRemote || hand != EnumHand.MAIN_HAND) return ActionResult(
            EnumActionResult.PASS,
            player.getHeldItem(hand)
        )
        return super.onItemRightClick(world, player, hand)
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
                val state = world.getBlockState(pos)
                val block = state.block

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
                    val eventFacing = if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
                        EnumFacing.NORTH
                    } else facing
                    for (entry in NEWMachineAssemblyManager.getConstructorsIterator()) {
                        if (entry.key.block == block && entry.key.meta == meta) {
                            m = entry.value
                            val pf = block.blockState.getProperty("facting")
                            e = if (pf != null) {
                                state.getValue(pf) as? EnumFacing ?: eventFacing
                            } else eventFacing
                            break
                        }
                    }
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

    fun isUsingAE(stack: ItemStack): Boolean {
        return openNbtData(stack).getBoolean("UsingAE")
    }

    fun isIgnoreFluids(stack: ItemStack): Boolean {
        return openNbtData(stack).getBoolean("IgnoreFluids")
    }

    fun getDynamicPatternSize(stack: ItemStack): Int {
        return openNbtData(stack).getInteger("DynamicPatternSize")
    }

    fun isAutoAECrafting(stack: ItemStack): Boolean {
        return openNbtData(stack).getBoolean("AutoAECrafting")
    }

    fun isNeedAllIngredient(stack: ItemStack): Boolean {
        return openNbtData(stack).getBoolean("NeedAllIngredient")
    }

    fun setUsingAE(stack: ItemStack, b: Boolean) {
        openNbtData(stack).setBoolean("UsingAE", b)
    }

    fun setIgnoreFluids(stack: ItemStack, b: Boolean) {
        openNbtData(stack).setBoolean("IgnoreFluids", b)
    }

    fun setDynamicPatternSize(stack: ItemStack, size: Int) {
        openNbtData(stack).setByte("DynamicPatternSize", size.toByte())
    }

    fun setAutoAECrafting(stack: ItemStack, auto: Boolean) {
        openNbtData(stack).setBoolean("AutoAECrafting", auto)
    }

    fun setNeedAllIngredient(stack: ItemStack, auto: Boolean) {
        openNbtData(stack).setBoolean("NeedAllIngredient", auto)
    }

}