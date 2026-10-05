package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import github.kasuminova.novaeng.common.network.bandwidth.NovaBandwidth;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraft.network.EnumPacketDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkDispatcher.class, remap = false)
public class MixinNetworkDispatcherBandwidth {
    @Shadow
    @Final
    public NetworkManager manager;

    @Inject(method = {"completeClientSideConnection", "completeServerSideConnection"}, at = @At("RETURN"))
    private void novaeng$afterForgeHandshake(NetworkDispatcher.ConnectionType type, CallbackInfo ci) {
        if (!manager.isLocalChannel()) {
            NovaBandwidth.start(manager);
        }
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void novaeng$useExpandedServerPayload(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, CallbackInfo ci) {
        if (!manager.isLocalChannel() && manager.getDirection() == EnumPacketDirection.SERVERBOUND && NovaBandwidth.active(ctx.channel())
            && msg instanceof FMLProxyPacket packet && packet.payload().readableBytes() <= NetworkLimits.payload(true)) {
            // Preserve the old multipart path for payloads above the configured ordinary-packet limit.
            // slice() preserves Forge's ownership and removes any consumed prefix from the size check.
            ctx.write(new SPacketCustomPayload(packet.channel(), new PacketBuffer(packet.payload().slice())), promise);
            ci.cancel();
        }
    }
}
