package github.kasuminova.novaeng.common.network.bandwidth;

import github.kasuminova.novaeng.NovaEngCoreConfig;

public final class NetworkLimits {
    private NetworkLimits() {
    }

    public static int payload(boolean clientbound) {
        return NovaEngCoreConfig.NETWORK.expandPacketLimits ? configuredPayload() : clientbound ? 1048576 : 32767;
    }

    public static int frame() {
        return NovaEngCoreConfig.NETWORK.expandPacketLimits ? configuredPayload() + BandwidthSettings.MIB : 2097151;
    }

    public static int decompressed() {
        return NovaEngCoreConfig.NETWORK.expandPacketLimits ? configuredPayload() + 1024 : 2097152;
    }

    public static BandwidthSettings settings() {
        var config = NovaEngCoreConfig.NETWORK;
        // A connection carries both directions. Vanilla 1.12.2 has asymmetric
        // CustomPayload limits (32,767 serverbound versus 1 MiB clientbound),
        // so a single per-connection validator must use the larger permitted
        // direction or it would reject valid clientbound traffic on the way out.
        int payloadLimit = Math.max(payload(false), payload(true));
        return new BandwidthSettings(payloadLimit, Math.clamp(config.compressionWindowLog, 21, 25), config.reuseCompressionContext);
    }

    private static int configuredPayload() {
        return Math.clamp(NovaEngCoreConfig.NETWORK.maxCustomPayloadMiB, 1, 64) * BandwidthSettings.MIB;
    }
}
