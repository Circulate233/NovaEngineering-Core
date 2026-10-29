package github.kasuminova.novaeng.common.network;

import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PktAutoCraftConfirm implements IMessage, IMessageHandler<PktAutoCraftConfirm, IMessage> {

    @Override
    public void fromBytes(final ByteBuf buf) {
    }

    @Override
    public void toBytes(final ByteBuf buf) {
    }

    @Override
    public IMessage onMessage(final PktAutoCraftConfirm message, final MessageContext ctx) {
        final var player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            if (player.openContainer instanceof ContainerNEWCraftConfirm) {
                final AutoCraftingQueue queue = AutoCraftingQueue.getQueue(player);
                if (queue != null && !queue.executionQueue(player)) {
                    player.closeContainer();
                }
            }
        });
        return null;
    }
}
