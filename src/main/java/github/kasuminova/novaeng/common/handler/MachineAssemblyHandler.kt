package github.kasuminova.novaeng.common.handler

import github.kasuminova.novaeng.NovaEngCoreConfig
import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool.getDynamicPatternSize
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import hellfirepvp.modularmachinery.client.ClientScheduler
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext
import hellfirepvp.modularmachinery.common.block.BlockController
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object MachineAssemblyHandler {

    @SubscribeEvent
    fun onPlayerRightBlock(event: PlayerInteractEvent.RightClickBlock) {
        val hand = event.hand
        if (hand != EnumHand.MAIN_HAND) return
        val world = event.world
        val player = event.entityPlayer
        val stack = player.getHeldItem(hand)
        if (!player.isSneaking && stack.item is ItemMachineAssemblyTool) {
            val pos = event.pos
            val tile = world.getTileEntity(pos)
            var state = world.getBlockState(pos)
            val block = state.block
            @Suppress("DEPRECATION")
            state = block.getActualState(state, world, pos)
            var facing: EnumFacing? = null
            val machine: DynamicMachine = if (tile is TileMultiblockMachineController) {
                tile.blueprintMachine ?: if (block is BlockController)
                    block.getParentMachine()
                else return
            } else {
                val meta = block.getMetaFromState(state)
                var m: DynamicMachine? = null
                for (entry in NEWMachineAssemblyManager.getConstructorsIterator()) {
                    if (entry.key == block && entry.value.containsKey(meta)) {
                        m = entry.value[meta]
                        val pf = block.blockState.getProperty("facing")
                        facing = if (pf != null) {
                            state.getValue(pf) as? EnumFacing ?: event.face
                        } else event.face
                        break
                    }
                }
                facing = if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
                    EnumFacing.NORTH
                } else facing
                m ?: return
            }

            event.isCanceled = true
            event.cancellationResult = EnumActionResult.SUCCESS

            if (world.isRemote) {
                if (NEWBlockArrayPreviewRenderHelper.work) {
                    val oldpos = NEWBlockArrayPreviewRenderHelper.key
                    NEWBlockArrayPreviewRenderHelper.unloadWorld()
                    if (oldpos == pos.toLong()) {
                        return
                    }
                }
                val renderContext = DynamicMachineRenderContext
                    .createContext(machine, getDynamicPatternSize(stack))
                renderContext.shiftSnap = ClientScheduler.getClientTick()
                NEWBlockArrayPreviewRenderHelper.startPreview(renderContext, pos, facing)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        val player = event.player
        val world = player.world
        if (player is EntityPlayerMP && world.worldTime % NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildSpeed == 0L) {
            val ma = NEWMachineAssemblyManager.getMachineAssembly(player) ?: return
            var restart: Boolean
            for (i in 0..<NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildQuantity) {
                do {
                    restart = false

                    when (ma.assemblyBlock(world, player)) {
                        NEWMachineAssemblyManager.OperatingStatus.ALREADY_EXISTS -> {
                            restart = true
                            continue
                        }

                        NEWMachineAssemblyManager.OperatingStatus.FAILURE -> {
                            NEWMachineAssemblyManager.removeMachineAssembly(player)
                            return
                        }

                        NEWMachineAssemblyManager.OperatingStatus.COMPLETE -> {
                            player.sendMessage(
                                TextComponentTranslation(
                                    "message.assembly.tip.success"
                                )
                            )
                            NEWMachineAssemblyManager.removeMachineAssembly(player)
                            return
                        }

                        NEWMachineAssemblyManager.OperatingStatus.SUCCESS -> {}
                    }
                } while (restart)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerLogOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        NEWMachineAssemblyManager.removeMachineAssembly(event.player)
    }

    @SubscribeEvent
    fun onPlayerChangeDim(event: PlayerEvent.PlayerChangedDimensionEvent) {
        NEWMachineAssemblyManager.removeMachineAssembly(event.player)
    }

}