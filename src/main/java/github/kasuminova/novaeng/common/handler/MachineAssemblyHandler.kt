package github.kasuminova.novaeng.common.handler

import github.kasuminova.novaeng.NovaEngCoreConfig
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool
import github.kasuminova.novaeng.common.util.NEWBlockArrayPreviewRenderHelper
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.OperatingStatus
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Mouse

object MachineAssemblyHandler {

    @SideOnly(Side.CLIENT)
    object Client {

        val mc: Minecraft = Minecraft.getMinecraft()

        @SubscribeEvent
        fun onMouseEvent(event: MouseEvent) {
            if (mc.player != null && mc.player.isSneaking() && NEWBlockArrayPreviewRenderHelper.work) {
                val stack: ItemStack = mc.player.heldItemMainhand
                var delta = -Mouse.getEventDWheel()
                if (delta % 120 == 0) {
                    delta /= 120
                }
                if (delta != 0 && stack.item is ItemMachineAssemblyTool) {
                    NEWBlockArrayPreviewRenderHelper
                        .setLayers(NEWBlockArrayPreviewRenderHelper.getLayers() + delta)
                    event.isCanceled = true
                }
            }
        }

        @SubscribeEvent
        fun tick(event: TickEvent.ClientTickEvent) {
            if (event.phase === TickEvent.Phase.END) {
                return
            }
            NEWBlockArrayPreviewRenderHelper.tick()
        }

        @SubscribeEvent
        fun onRenderLast(event: RenderWorldLastEvent) {
            if (Minecraft.getMinecraft().player == null) return
            NEWBlockArrayPreviewRenderHelper.renderTranslucentBlocks()
        }

        @SubscribeEvent
        fun purgeDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
            NEWBlockArrayPreviewRenderHelper.unloadWorld()
        }

        @SubscribeEvent
        fun onWorldChange(unload: WorldEvent.Unload) {
            if (unload.world.isRemote) {
                NEWBlockArrayPreviewRenderHelper.unloadWorld()
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        val player = event.player
        val world = player.world
        if (player is EntityPlayerMP && world.worldTime % NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildSpeed == 0L) {
            val ma = NEWMachineAssemblyManager.getMachineAssembly(player) ?: return
            for (i in 0..<NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildQuantity) {
                when (ma.assemblyBlock(world, player)) {
                    OperatingStatus.FAILURE -> {
                        NEWMachineAssemblyManager.removeMachineAssembly(player)
                        return
                    }

                    OperatingStatus.COMPLETE -> {
                        player.sendMessage(
                            TextComponentTranslation(
                                "message.assembly.tip.success"
                            )
                        )
                        NEWMachineAssemblyManager.removeMachineAssembly(player)
                        return
                    }

                    else -> {}
                }
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