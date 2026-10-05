package github.kasuminova.novaeng.common.network.bandwidth;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketDisconnect;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketRespawn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;


public final class NovaBandwidth {
    public static final String INBOUND = "novaeng:bandwidth_in";
    public static final String OUTBOUND = "novaeng:bandwidth_out";
    private static final AttributeKey<BandwidthConnection> CONNECTION = AttributeKey.valueOf("novaeng:bandwidth");
    private static final AttributeKey<Integer> INSTALL_RETRIES = AttributeKey.valueOf("novaeng:bandwidth_install_retries");
    private static final AttributeKey<Boolean> INSTALL_RETRY_SCHEDULED = AttributeKey.valueOf("novaeng:bandwidth_install_retry_scheduled");
    private static final AttributeKey<Boolean> LOCAL_BYPASS_RECORDED = AttributeKey.valueOf("novaeng:bandwidth_local_bypass_recorded");
    private static final AttributeKey<Boolean> START_RETRY_SCHEDULED = AttributeKey.valueOf("novaeng:bandwidth_start_retry_scheduled");
    private static final AttributeKey<Integer> START_RETRIES = AttributeKey.valueOf("novaeng:bandwidth_start_retries");
    private static final int MAX_INSTALL_RETRIES = 200;
    private static final int MAX_START_RETRIES = 200;
    private static final Logger LOG = LogManager.getLogger("NovaBandwidth");

    private NovaBandwidth() {
    }

    /** Install before login so a peer's first aggregated packet can always be decoded. */
    public static void install(NetworkManager manager) {
        var config = NovaEngCoreConfig.NETWORK;
        // Apply the listener setting before classifying the channel. LocalChannel is
        // deliberately excluded from all byte processing, but its bypass may still be
        // reported when monitoring is enabled.
        BandwidthStats.setEnabled(config.enableBandwidthMonitoring);
        Channel channel = manager.channel();
        if (channel == null) {
            return;
        }
        if (!channel.eventLoop().inEventLoop()) {
            channel.eventLoop().execute(() -> install(manager));
            return;
        }
        if (channel.attr(CONNECTION).get() != null) {
            return;
        }
        if (manager.isLocalChannel()) {
            // Integrated-server LocalChannel transports Packet objects directly. There is
            // no frame or serialized CustomPayload to aggregate, so the complete byte
            // transport chain stays disabled for pure single-player worlds. When the
            // integrated server is opened to LAN, its remote clients use TCP channels and
            // take the normal path below; the local player's channel remains packet-only.
            if (channel.attr(LOCAL_BYPASS_RECORDED).setIfAbsent(Boolean.TRUE) == null) {
                BandwidthStats.localTransportBypassed();
                LOG.info("Skipped byte-level bandwidth transport for LocalChannel {}; pipeline={} (local packets are not serialized)",
                    channel.remoteAddress(), channel.pipeline().names());
            }
            return;
        }
        BandwidthDictionary.setEnabled(config.enableBandwidthDictionary);
        ChannelPipeline pipeline = channel.pipeline();
        String decoder = handlerName(pipeline, "decoder", NettyPacketDecoder.class);
        String encoder = handlerName(pipeline, "encoder", NettyPacketEncoder.class);
        if (decoder == null || encoder == null) {
            scheduleInstallRetry(manager);
            return;
        }
        boolean server = manager.getDirection() == EnumPacketDirection.SERVERBOUND;
        EnumPacketDirection outgoing = server ? EnumPacketDirection.CLIENTBOUND : EnumPacketDirection.SERVERBOUND;
        int clientPayloadId = packetId(EnumPacketDirection.CLIENTBOUND, new SPacketCustomPayload());
        int serverPayloadId = packetId(EnumPacketDirection.SERVERBOUND, new CPacketCustomPayload());
        IntSet immediate = new IntOpenHashSet();
        if (server) {
            immediate.add(packetId(outgoing, new SPacketKeepAlive()));
            immediate.add(packetId(outgoing, new SPacketDisconnect()));
            immediate.add(packetId(outgoing, new SPacketRespawn()));
        } else {
            immediate.add(packetId(outgoing, new CPacketKeepAlive()));
            immediate.add(packetId(outgoing, new CPacketConfirmTeleport()));
        }
        ObjectSet<String> channels = new ObjectOpenHashSet<>(config.unaggregatedChannels);
        // Forge's protocol messages must never become subject to batching, even if config is edited.
        channels.add("FML|HS");
        channels.add("REGISTER");
        channels.add("UNREGISTER");
        BandwidthConnection connection = new BandwidthConnection(NetworkLimits.settings(),
            server ? serverPayloadId : clientPayloadId, server ? clientPayloadId : serverPayloadId,
            config.aggregationDelayMillis, Math.clamp(config.aggregationTargetKiB, 4, 1024) * 1024, server,
            immediate, channels, () -> channel.attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get() == EnumConnectionState.PLAY);
        channel.attr(CONNECTION).set(connection);
        channel.attr(INSTALL_RETRIES).set(null);
        channel.attr(INSTALL_RETRY_SCHEDULED).set(null);
        channel.attr(START_RETRIES).set(null);
        channel.attr(START_RETRY_SCHEDULED).set(null);
        connection.indexChannels = config.indexCustomPayloadChannels;
        BandwidthStats.connectionOpened();
        boolean inboundAdded = false;
        boolean outboundAdded = false;
        try {
            pipeline.addBefore(decoder, INBOUND, new BandwidthInboundHandler(connection));
            inboundAdded = true;
            pipeline.addBefore(encoder, OUTBOUND, connection.outbound);
            outboundAdded = true;
        } catch (Throwable failure) {
            if (inboundAdded && pipeline.get(INBOUND) != null) {
                pipeline.remove(INBOUND);
            }
            if (outboundAdded && pipeline.get(OUTBOUND) != null) {
                pipeline.remove(OUTBOUND);
            }
            channel.attr(CONNECTION).set(null);
            connection.close();
            BandwidthStats.connectionClosed();
            throw failure;
        }
        LOG.info("Installed bandwidth transport on {} (direction={}, decoder={}, encoder={}, pipeline={})",
            channel.remoteAddress(), manager.getDirection(), decoder, encoder, pipeline.names());
        channel.closeFuture().addListener(ignored -> {
            connection.close();
            BandwidthStats.connectionClosed();
            LOG.debug("Closed bandwidth connection {}: {}", channel.remoteAddress(), connection.statistics());
            channel.attr(CONNECTION).set(null);
        });
    }

    private static String handlerName(ChannelPipeline pipeline, String preferred, Class<?> type) {
        if (pipeline.get(preferred) != null) {
            return preferred;
        }
        for (String name : pipeline.names()) {
            if (type.isInstance(pipeline.get(name))) {
                return name;
            }
        }
        return null;
    }

    private static void scheduleInstallRetry(NetworkManager manager) {
        Channel channel = manager.channel();
        if (channel == null || !channel.isOpen()
            || Boolean.TRUE.equals(channel.attr(INSTALL_RETRY_SCHEDULED).get())) {
            return;
        }
        int retries = channel.attr(INSTALL_RETRIES).get() == null ? 0 : channel.attr(INSTALL_RETRIES).get();
        if (retries >= MAX_INSTALL_RETRIES) {
            LOG.warn("Unable to install bandwidth transport on {}; decoder/encoder never appeared; pipeline={}",
                channel.remoteAddress(), channel.pipeline().names());
            return;
        }
        channel.attr(INSTALL_RETRIES).set(retries + 1);
        channel.attr(INSTALL_RETRY_SCHEDULED).set(Boolean.TRUE);
        channel.eventLoop().schedule(() -> {
            channel.attr(INSTALL_RETRY_SCHEDULED).set(null);
            install(manager);
        }, 50, TimeUnit.MILLISECONDS);
    }

    public static void start(NetworkManager manager) {
        Channel channel = manager.channel();
        if (channel == null) {
            return;
        }
        Runnable start = () -> {
            if (manager.isLocalChannel()) {
                return;
            }
            if (channel.attr(CONNECTION).get() == null) {
                install(manager);
            }
            BandwidthConnection connection = channel.attr(CONNECTION).get();
            if (connection != null) {
                channel.attr(START_RETRIES).set(null);
                channel.attr(START_RETRY_SCHEDULED).set(null);
                boolean optimize = NovaEngCoreConfig.NETWORK.enableBandwidthOptimization
                    && NovaEngCoreConfig.NETWORK.expandPacketLimits;
                // Every completed connection refreshes the client dictionary. When the
                // planner is disabled the server still returns the empty version-zero dictionary.
                connection.startAggregation(optimize, NovaEngCoreConfig.NETWORK.enableBandwidthDictionary);
                LOG.info("Initialized bandwidth synchronization for {} (optimization={}, dictionary={})",
                    channel.remoteAddress(), optimize, NovaEngCoreConfig.NETWORK.enableBandwidthDictionary);
            } else {
                scheduleStartRetry(manager);
            }
        };
        if (channel.eventLoop().inEventLoop()) {
            start.run();
        } else {
            channel.eventLoop().execute(start);
        }
    }

    private static void scheduleStartRetry(NetworkManager manager) {
        Channel channel = manager.channel();
        if (channel == null || !channel.isOpen()
            || Boolean.TRUE.equals(channel.attr(START_RETRY_SCHEDULED).get())) {
            return;
        }
        int retries = channel.attr(START_RETRIES).get() == null ? 0 : channel.attr(START_RETRIES).get();
        if (retries >= MAX_START_RETRIES) {
            LOG.warn("Unable to initialize bandwidth synchronization on {}; transport was not installed; pipeline={}",
                channel.remoteAddress(), channel.pipeline().names());
            return;
        }
        channel.attr(START_RETRIES).set(retries + 1);
        channel.attr(START_RETRY_SCHEDULED).set(Boolean.TRUE);
        channel.eventLoop().schedule(() -> {
            channel.attr(START_RETRY_SCHEDULED).set(null);
            start(manager);
        }, 50, TimeUnit.MILLISECONDS);
    }

    public static boolean active(Channel channel) {
        BandwidthConnection connection = channel.attr(CONNECTION).get();
        return connection != null && connection.isActive();
    }

    public static boolean bypassZlib(ChannelHandlerContext context, ByteBuf packet) {
        BandwidthConnection connection = context.channel().attr(CONNECTION).get();
        return connection != null && connection.isOutgoingEnvelope(packet);
    }

    /** Compression is installed later during login, outside our handlers and inside framing. */
    public static String compressionAnchor(NetworkManager manager, String original) {
        if (manager.channel().attr(CONNECTION).get() == null) {
            return original;
        }
        return switch (original) {
            case "decoder" -> INBOUND;
            case "encoder" -> OUTBOUND;
            default -> original;
        };
    }

    private static int packetId(EnumPacketDirection direction, Packet<?> packet) {
        try {
            Integer id = EnumConnectionState.PLAY.getPacketId(direction, packet);
            if (id == null) {
                throw new IllegalStateException("Missing vanilla packet id for " + packet.getClass().getName());
            }
            return id;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot resolve vanilla packet id for " + packet.getClass().getName(), failure);
        }
    }
}
