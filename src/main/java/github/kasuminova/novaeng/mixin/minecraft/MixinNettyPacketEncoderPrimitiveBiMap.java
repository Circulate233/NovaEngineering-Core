package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.PacketIdAccess;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(NettyPacketEncoder.class)
public abstract class MixinNettyPacketEncoderPrimitiveBiMap {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private static Marker RECEIVED_PACKET_MARKER;
    @Shadow
    @Final
    private EnumPacketDirection direction;

    /**
     * @author NovaEngineering
     * @reason Carry the primitive packet discriminator directly into the network buffer.
     */
    @Overwrite
    protected void encode(
        final ChannelHandlerContext context, final Packet<?> packet, final ByteBuf output)
        throws Exception {
        final EnumConnectionState state =
            context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get();
        if (state == null) throw new RuntimeException("ConnectionProtocol unknown: " + packet);
        final int id = ((PacketIdAccess) (Object) state).nova$getPacketId(direction, packet);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                RECEIVED_PACKET_MARKER,
                "OUT: [{}:{}] {}",
                context.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get(),
                id == -1 ? "null" : Integer.toString(id),
                packet.getClass().getName());
        }
        if (id == -1) throw new IOException("Can't serialize unregistered packet");
        final PacketBuffer buffer = new PacketBuffer(output);
        buffer.writeVarInt(id);
        packet.writePacketData(buffer);
    }
}
