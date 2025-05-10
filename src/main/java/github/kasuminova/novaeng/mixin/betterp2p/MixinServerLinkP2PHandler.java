package github.kasuminova.novaeng.mixin.betterp2p;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.packet.C2SLinkP2P;
import com.projecturanus.betterp2p.network.packet.ServerLinkP2PHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ServerLinkP2PHandler.class,remap = false)
public class MixinServerLinkP2PHandler {

    /**
     * @author circulation
     * @reason Fix CME caused by updating ME network in non-server thread.
     */
    @Overwrite
    @Nullable
    public IMessage onMessage(C2SLinkP2P message, MessageContext ctx) {
        if (message.getInput() == null || message.getOutput() == null) {
            return null;
        }

        var player = ctx.getServerHandler().player;
        var state = ModNetwork.INSTANCE.getPlayerState().get(player.getUniqueID());
        if (state == null){
            return null;
        }
        player.getServer().addScheduledTask(() -> {
            var result = state.getGridCache().linkP2P(message.getInput(), message.getOutput());
            if (result != null) {
                ModNetwork.INSTANCE.requestP2PUpdate(player);
            }
        });

        return null;
    }
}
