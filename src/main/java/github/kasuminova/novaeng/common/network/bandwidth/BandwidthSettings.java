package github.kasuminova.novaeng.common.network.bandwidth;

/** Immutable settings captured per connection; configuration changes cannot reset a live stream. */
public record BandwidthSettings(int payloadLimit, int windowLog, boolean reuseContext) {
    public static final int MIB = 1024 * 1024;
    public static final int MAX_PAYLOAD = 64 * MIB;
    public static final int MAX_PACKETS = 1024;

    public BandwidthSettings {
        if (payloadLimit < 32767 || payloadLimit > MAX_PAYLOAD || windowLog < 21 || windowLog > 25) {
            throw new IllegalArgumentException("Invalid bandwidth limits");
        }
    }

    public int packetLimit() {
        return payloadLimit + 256;
    }

    public int batchLimit() {
        return payloadLimit + 1024;
    }

    public int frameLimit() {
        return payloadLimit + MIB;
    }
}
