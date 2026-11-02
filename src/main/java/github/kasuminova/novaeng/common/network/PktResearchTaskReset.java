package github.kasuminova.novaeng.common.network;

import github.kasuminova.novaeng.common.container.ContainerHyperNetTerminal;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchStation;
import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PktResearchTaskReset implements IMessage, IMessageHandler<PktResearchTaskReset, IMessage> {

    @Override
    public void fromBytes(final ByteBuf buf) {
    }

    @Override
    public void toBytes(final ByteBuf buf) {
    }

    @Override
    public IMessage onMessage(final PktResearchTaskReset message, final MessageContext ctx) {
        final var player = ctx.getServerHandler().player;
        if (!(player.openContainer instanceof ContainerHyperNetTerminal container)) {
            return null;
        }
        final var center = container.getOwner().getNodeProxy().getCenter();
        if (center == null) {
            return null;
        }
        for (final ResearchStation station : center.getNode(ResearchStation.class)) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> station.provideTask(null, null));
        }
        return null;
    }
}
