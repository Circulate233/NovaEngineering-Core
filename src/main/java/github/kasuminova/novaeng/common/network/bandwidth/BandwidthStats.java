package github.kasuminova.novaeng.common.network.bandwidth;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.util.text.TextFormatting;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Counters for comparing the same workload with Nova optimization enabled and disabled. */
public final class BandwidthStats {
    private static final AtomicLong OUT_RAW = new AtomicLong();
    private static final AtomicLong OUT_WIRE = new AtomicLong();
    private static final AtomicLong IN_RAW = new AtomicLong();
    private static final AtomicLong IN_WIRE = new AtomicLong();
    private static final AtomicLong OUT_PACKETS = new AtomicLong();
    private static final AtomicLong IN_PACKETS = new AtomicLong();
    private static final AtomicLong OUT_BATCHES = new AtomicLong();
    private static final AtomicLong IN_BATCHES = new AtomicLong();
    private static final AtomicLong INDEX_DELTA = new AtomicLong();
    private static final AtomicLong CONNECTIONS = new AtomicLong();
    private static final AtomicLong LOCAL_PACKET_CONNECTIONS = new AtomicLong();
    private static final AtomicBoolean SUMMARY_LOGGED = new AtomicBoolean();
    // Keep collection off until the common/client lifecycle has applied the config.
    // This prevents early channel setup from collecting data before the first world/server event.
    private static volatile boolean enabled;

    private BandwidthStats() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void connectionOpened() {
        if (enabled()) {
            CONNECTIONS.incrementAndGet();
        }
    }

    public static void connectionClosed() {
        if (enabled()) {
            CONNECTIONS.updateAndGet(value -> Math.max(0, value - 1));
        }
    }

    public static void localTransportBypassed() {
        if (enabled()) {
            LOCAL_PACKET_CONNECTIONS.incrementAndGet();
        }
    }

    public static void outgoingPacket(int bytes) {
        if (!enabled()) return;
        OUT_RAW.addAndGet(bytes);
        OUT_WIRE.addAndGet(bytes);
        OUT_PACKETS.incrementAndGet();
    }

    public static void incomingPacket(int bytes) {
        if (!enabled()) return;
        IN_RAW.addAndGet(bytes);
        IN_WIRE.addAndGet(bytes);
        IN_PACKETS.incrementAndGet();
    }

    public static void outgoingBatch(int rawBytes, int wireBytes, int packets) {
        if (!enabled()) return;
        OUT_RAW.addAndGet(rawBytes);
        OUT_WIRE.addAndGet(wireBytes);
        OUT_PACKETS.addAndGet(packets);
        OUT_BATCHES.incrementAndGet();
    }

    public static void incomingBatch(int rawBytes, int wireBytes, int packets) {
        if (!enabled()) return;
        IN_RAW.addAndGet(rawBytes);
        IN_WIRE.addAndGet(wireBytes);
        IN_PACKETS.addAndGet(packets);
        IN_BATCHES.incrementAndGet();
    }

    public static void indexedDelta(int bytes) {
        if (!enabled()) return;
        INDEX_DELTA.addAndGet(bytes);
    }

    public static void reset() {
        OUT_RAW.set(0);
        OUT_WIRE.set(0);
        IN_RAW.set(0);
        IN_WIRE.set(0);
        OUT_PACKETS.set(0);
        IN_PACKETS.set(0);
        OUT_BATCHES.set(0);
        IN_BATCHES.set(0);
        INDEX_DELTA.set(0);
        LOCAL_PACKET_CONNECTIONS.set(0);
        SUMMARY_LOGGED.set(false);
    }

    public static ObjectList<String> messages() {
        if (!enabled()) {
            return ObjectLists.singleton(TextFormatting.GRAY + "Nova bandwidth listener is disabled.");
        }
        long outRaw = OUT_RAW.get();
        long outWire = OUT_WIRE.get();
        long inRaw = IN_RAW.get();
        long inWire = IN_WIRE.get();
        ObjectList<String> result = new ObjectArrayList<>(6);
        result.add(TextFormatting.AQUA + "Nova bandwidth listener (always on)");
        result.add("connections=" + CONNECTIONS.get() + ", local packet-only connections=" + LOCAL_PACKET_CONNECTIONS.get()
            + ", optimization=" + (NovaEngCoreConfig.NETWORK.enableBandwidthOptimization ? "on" : "off"));
        result.add("OUT raw=" + outRaw + "B wire=" + outWire + "B saved=" + saved(outRaw, outWire)
            + "B (" + savedRatio(outRaw, outWire) + " saved; wire=" + wireRatio(outRaw, outWire)
            + " of raw) packets=" + OUT_PACKETS.get() + " batches=" + OUT_BATCHES.get());
        result.add("IN  raw=" + inRaw + "B wire=" + inWire + "B saved=" + saved(inRaw, inWire)
            + "B (" + savedRatio(inRaw, inWire) + " saved; wire=" + wireRatio(inRaw, inWire)
            + " of raw) packets=" + IN_PACKETS.get() + " batches=" + IN_BATCHES.get());
        result.add("indexed channel header delta=" + INDEX_DELTA.get() + "B (positive saves, negative expands)");
        result.add("raw = before Nova batching/compression; wire = Nova envelope payload, excluding TCP/IP and encryption");
        if (LOCAL_PACKET_CONNECTIONS.get() != 0) {
            result.add("local packet-only connections are not serialized by Netty; byte-level batching/compression is unavailable");
        }
        return ObjectLists.unmodifiable(result);
    }

    /**
     * Returns whether the counters would describe transport expansion. Kept package-private so
     * the transport tests can verify the listener invariant without loading Minecraft's chat
     * formatting classes in the plain Netty test runtime.
     */
    static boolean hasExpansion() {
        return OUT_WIRE.get() > OUT_RAW.get() || IN_WIRE.get() > IN_RAW.get();
    }

    public static void logSummary(String reason) {
        if (!enabled() || !SUMMARY_LOGGED.compareAndSet(false, true)) {
            return;
        }
        NovaEngineeringCore.log.info("Nova bandwidth summary ({})", reason);
        for (String message : messages()) {
            NovaEngineeringCore.log.info(message);
        }
    }

    private static boolean enabled() {
        return enabled;
    }

    private static long saved(long raw, long wire) {
        return raw - wire;
    }

    private static String savedRatio(long raw, long wire) {
        if (raw <= 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.2f%%", (raw - wire) * 100.0 / raw);
    }

    private static String wireRatio(long raw, long wire) {
        if (raw <= 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.2f%%", wire * 100.0 / raw);
    }
}
