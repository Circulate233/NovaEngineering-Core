package github.kasuminova.novaeng.common.network

import github.kasuminova.novaeng.common.container.ContainerHyperNetTerminal
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchStation
import hellfirepvp.modularmachinery.ModularMachinery
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class PktResearchTaskReset : IMessage, IMessageHandler<PktResearchTaskReset?, IMessage?> {
    override fun fromBytes(buf: ByteBuf?) {
    }

    override fun toBytes(buf: ByteBuf?) {
    }

    override fun onMessage(message: PktResearchTaskReset?, ctx: MessageContext): IMessage? {
        val player = ctx.serverHandler.player
        val container = player.openContainer
        if (container !is ContainerHyperNetTerminal) {
            return null
        }

        val terminal = container.getOwner()

        val nodeProxy = terminal.nodeProxy
        val center = nodeProxy.getCenter() ?: return null

        for (station in center.getNode(ResearchStation::class.java)) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask { station.provideTask(null, null) }
        }
        return null
    }
}
