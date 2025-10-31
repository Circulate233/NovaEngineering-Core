package github.kasuminova.novaeng.common.network

import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm
import github.kasuminova.novaeng.common.util.AutoCraftingQueue
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class PktAutoCraftConfirm : IMessage, IMessageHandler<PktAutoCraftConfirm, IMessage> {
    override fun fromBytes(buf: ByteBuf) {

    }

    override fun toBytes(buf: ByteBuf) {

    }

    override fun onMessage(
        message: PktAutoCraftConfirm,
        ctx: MessageContext
    ): IMessage? {
        val player = ctx.serverHandler.player
        if (player.openContainer is ContainerNEWCraftConfirm) {
            AutoCraftingQueue.getQueue(player)?.let {
                if (!it.executionQueue(player)) {
                    player.closeContainer()
                }
            }
        }
        return null
    }
}