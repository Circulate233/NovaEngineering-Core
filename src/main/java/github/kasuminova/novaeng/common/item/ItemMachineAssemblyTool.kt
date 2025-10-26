package github.kasuminova.novaeng.common.item

import appeng.util.Platform
import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import hellfirepvp.modularmachinery.client.ClientScheduler
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.block.BlockFactoryController
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import hellfirepvp.modularmachinery.common.util.BlockArrayCache
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
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
        if (hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS
        val state = world.getBlockState(pos)
        val block = state.block
        val tile = world.getTileEntity(pos)
        val stack = player.getHeldItem(hand)
        if (tile == null) {
            return EnumActionResult.PASS
        } else if (player.isSneaking) {
            val mode = getMode(stack)
            if (!world.isRemote && mode == 0.toByte()) {
                if (!NEWMachineAssemblyManager.checkMachineAssembly(player)) {
                    var array: NEWMachineAssemblyManager.AssemblyBlockArray? = null
                    if (tile is TileMultiblockMachineController) {
                        var machine = tile.blueprintMachine
                        if (machine == null) {
                            if (block is BlockController) {
                                machine = block.getParentMachine()
                            }
                            if (block is BlockFactoryController) {
                                machine = block.getParentMachine()
                            }
                        }
                        if (machine == null) return EnumActionResult.FAIL

                        val controllerFacing = player.world.getBlockState(pos).getValue(BlockController.FACING)
                        array = NEWMachineAssemblyManager.AssemblyBlockArray(
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
                    } else {
                        for (entry in NEWMachineAssemblyManager.getConstructorsIterator()) {
                            if (entry.key.isInstance(tile)) {
                                array = entry.value[facing]
                                break
                            }
                        }
                    }

                    array?.let {
                        array = array.offset(pos)
                        if (array.min.y < 1 || array.max.y > 255) {
                            val e = if (array.min.y < 1) {
                                "y = ${array.min.y}"
                            } else {
                                "y = ${array.max.y}"
                            }
                            player.sendMessage(
                                TextComponentTranslation(
                                    "message.assembly.tip.too_high", e
                                )
                            )
                            return EnumActionResult.FAIL
                        }
                        array = NEWMachineAssemblyManager.addAssemblyMachine(player, array)
                        array.usingAE = isUsingAE(stack)
                        array.ignoreFluids = isIgnoreFluids(stack)
                        array.start()
                        //TODO:开始工作的通知
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
            } else if (world.isRemote && mode == 1.toByte()) {
                var machine: DynamicMachine? = null
                if (tile is TileMultiblockMachineController) {
                    machine = tile.blueprintMachine
                    if (machine == null) {
                        if (block is BlockController) {
                            machine = block.getParentMachine()
                        }
                        if (block is BlockFactoryController) {
                            machine = block.getParentMachine()
                        }
                    }
                }
                if (machine != null) {
                    val renderContext = DynamicMachineRenderContext
                        .createContext(machine, getDynamicPatternSize(stack))
                    renderContext.shiftSnap = ClientScheduler.getClientTick()
                    NEWBlockArrayPreviewRenderHelper.startPreview(renderContext, pos)
                }
            }
        }
        return EnumActionResult.PASS
    }

    fun isUsingAE(stack: ItemStack): Boolean {
        return Platform.openNbtData(stack).getBoolean("UsingAE")
    }

    fun isIgnoreFluids(stack: ItemStack): Boolean {
        return Platform.openNbtData(stack).getBoolean("IgnoreFluids")
    }

    fun getDynamicPatternSize(stack: ItemStack): Int {
        return Platform.openNbtData(stack).getInteger("DynamicPatternSize")
    }

    fun getMode(stack: ItemStack): Byte {
        return Platform.openNbtData(stack).getByte("Mode")
    }

    fun setUsingAE(stack: ItemStack, b: Boolean) {
        return Platform.openNbtData(stack).setBoolean("UsingAE", b)
    }

    fun setIgnoreFluids(stack: ItemStack, b: Boolean) {
        return Platform.openNbtData(stack).setBoolean("IgnoreFluids", b)
    }

    fun setDynamicPatternSize(stack: ItemStack, size: Int) {
        return Platform.openNbtData(stack).setByte("DynamicPatternSize", size.toByte())
    }

    fun setMode(stack: ItemStack, mode: Byte) {
        return Platform.openNbtData(stack).setByte("Mode", mode)
    }

}