package github.kasuminova.novaeng.common.network;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.util.Ae2Reflect;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CPacketSwitchGuis implements IMessage {
    private GuiType guiType;

    public CPacketSwitchGuis(GuiType guiType) {
        this.guiType = guiType;
    }

    public CPacketSwitchGuis() {
    }

    public void fromBytes(ByteBuf byteBuf) {
        this.guiType = GuiType.getByOrdinal(byteBuf.readByte());
    }

    public void toBytes(ByteBuf byteBuf) {
        byteBuf.writeByte(this.guiType != null ? this.guiType.ordinal() : 0);
    }

    public static class Handler implements IMessageHandler<CPacketSwitchGuis, IMessage> {
        public Handler() {
        }

        @Nullable
        public IMessage onMessage(CPacketSwitchGuis message, MessageContext ctx) {
            if (message.guiType != null) {
                EntityPlayerMP player = ctx.getServerHandler().player;
                Container cont = player.openContainer;
                if (cont instanceof AEBaseContainer) {
                    ContainerOpenContext context = ((AEBaseContainer) cont).getOpenContext();
                    if (context != null) {
                        player.getServerWorld().addScheduledTask(() ->
                                InventoryHandler.openGui(
                                        player,
                                        player.world,
                                        new BlockPos(Ae2Reflect.getContextX(context),
                                                Ae2Reflect.getContextY(context),
                                                Ae2Reflect.getContextZ(context)),
                                        context.getSide().getFacing(),
                                        message.guiType
                                ));
                    }
                }
            }
            return null;
        }
    }
}

