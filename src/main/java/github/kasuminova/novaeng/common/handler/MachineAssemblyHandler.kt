package github.kasuminova.novaeng.common.handler

import github.kasuminova.novaeng.NovaEngCoreConfig
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object MachineAssemblyHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        val player = event.player
        val world = player.world
        if (player is EntityPlayerMP && world.worldTime % NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildSpeed == 0L) {
            val ma = NEWMachineAssemblyManager.getMachineAssembly(player) ?: return
            for (i in 0..<NovaEngCoreConfig.MACHINE_ASSEMBLY_TOOL.buildQuantity) {
                when (ma.assemblyBlock(world, player)) {
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