package github.kasuminova.novaeng.mixin.betterp2p;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.packet.C2SUnlinkP2P;
import com.projecturanus.betterp2p.network.packet.S2COpenGui;
import com.projecturanus.betterp2p.network.packet.ServerUnlinkP2PHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ServerUnlinkP2PHandler.class, remap = false)
public class MixinServerUnlinkP2PHandler {

    /**
     * @author circulation
     * @reason Fix CME caused by updating ME network in non-server thread.
     */
    @Overwrite
    @Nullable
    public S2COpenGui onMessage(C2SUnlinkP2P message, MessageContext ctx) {
        if (message.getP2p() == null) {
            return null;
        }

        var player = ctx.getServerHandler().player;
        var cache = ModNetwork.INSTANCE.getPlayerState().get(player.getUniqueID());
        if (cache == null) {
            return null;
        }
        player.getServer().addScheduledTask(() -> {
            cache.getGridCache().unlinkP2P(message.getP2p());
            ModNetwork.INSTANCE.requestP2PUpdate(player);
        });

        return null;
    }
}
