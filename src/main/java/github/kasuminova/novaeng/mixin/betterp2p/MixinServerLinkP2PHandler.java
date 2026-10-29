package github.kasuminova.novaeng.mixin.betterp2p;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.projecturanus.betterp2p.network.packet.C2SLinkP2P;
import com.projecturanus.betterp2p.network.packet.ServerLinkP2PHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Objects;

@Mixin(value = ServerLinkP2PHandler.class, remap = false)
public abstract class MixinServerLinkP2PHandler {

    @WrapMethod(method = "onMessage(Lcom/projecturanus/betterp2p/network/packet/C2SLinkP2P;Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;")
    private IMessage nova$linkOnServerThread(final C2SLinkP2P message, final MessageContext ctx,
                                             final Operation<IMessage> original) {
        if (message.getInput() == null || message.getOutput() == null) {
            return null;
        }
        Objects.requireNonNull(ctx.getServerHandler().player.getServer()).addScheduledTask(() -> original.call(message, ctx));
        return null;
    }
}
