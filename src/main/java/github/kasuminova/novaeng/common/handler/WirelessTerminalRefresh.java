package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.registry.RegistryItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class WirelessTerminalRefresh implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<WirelessTerminalRefresh, IMessage> {
        @Override
        public IMessage onMessage(WirelessTerminalRefresh message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChangeB(player));
            return null;
        }
    }
}
