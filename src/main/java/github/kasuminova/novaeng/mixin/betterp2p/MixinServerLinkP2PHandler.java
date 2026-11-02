package github.kasuminova.novaeng.mixin.betterp2p;

import com.projecturanus.betterp2p.network.packet.C2SLinkP2P;
import com.projecturanus.betterp2p.network.packet.ServerLinkP2PHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = ServerLinkP2PHandler.class, remap = false)
public abstract class MixinServerLinkP2PHandler {

    @Unique
    private static final ThreadLocal<C2SLinkP2P> NOVA$SERVER_DISPATCH = new ThreadLocal<>();

    @Inject(method = "onMessage(Lcom/projecturanus/betterp2p/network/packet/C2SLinkP2P;Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;",
        at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void nova$linkOnServerThread(final C2SLinkP2P message, final MessageContext ctx,
                                         final CallbackInfoReturnable<IMessage> cir) {
        if (NOVA$SERVER_DISPATCH.get() == message) {
            return;
        }
        if (message.getInput() == null || message.getOutput() == null) {
            cir.setReturnValue(null);
            return;
        }
        Objects.requireNonNull(ctx.getServerHandler().player.getServer()).addScheduledTask(() -> {
            final C2SLinkP2P previous = NOVA$SERVER_DISPATCH.get();
            NOVA$SERVER_DISPATCH.set(message);
            try {
                ((ServerLinkP2PHandler) (Object) this).onMessage(message, ctx);
            } finally {
                if (previous == null) {
                    NOVA$SERVER_DISPATCH.remove();
                } else {
                    NOVA$SERVER_DISPATCH.set(previous);
                }
            }
        });
        cir.setReturnValue(null);
    }
}
