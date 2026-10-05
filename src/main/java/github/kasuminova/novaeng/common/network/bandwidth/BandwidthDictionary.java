package github.kasuminova.novaeng.common.network.bandwidth;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Server-owned dictionary planner. Snapshots are immutable for the lifetime of a connection. */
public final class BandwidthDictionary {
    public static final int MAX_DICTIONARY_BYTES = 32 * 1024;
    public static final int MAX_SAMPLE_BYTES = 16 * 1024;
    private static final int MIN_SAMPLES = 32;
    private static final int MIN_SAMPLE_BYTES = 64 * 1024;
    private static final int MAX_SAMPLES = 128;
    private static final int MAX_BUFFERED_BYTES = 512 * 1024;
    private static final long MIN_RETRAIN_NANOS = 60_000_000_000L;
    private static final int MIN_PUBLISH_BYTES = 512;
    private static final int MIN_IMPROVEMENT_PERCENT = 2;
    private static final Logger LOG = Logger.getLogger("NovaBandwidthDictionary");
    private static final ExecutorService TRAINER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NovaBandwidthDictionary");
        thread.setDaemon(true);
        return thread;
    });
    private static final Object LOCK = new Object();
    private static volatile Snapshot current = new Snapshot(0, new byte[0]);
    private static ObjectList<byte[]> samples = new ObjectArrayList<>();
    private static int sampleBytes;
    private static long lastTraining;
    private static long generation;
    private static boolean training;
    private static volatile boolean enabled = true;

    private BandwidthDictionary() {
    }

    public static void setEnabled(boolean value) {
        synchronized (LOCK) {
            if (!value && enabled) {
                generation++;
                current = new Snapshot(0, new byte[0]);
                samples = new ObjectArrayList<>();
                sampleBytes = 0;
                lastTraining = 0;
                training = false;
            }
            enabled = value;
        }
    }

    public static Snapshot snapshot() {
        return current;
    }

    public static void observe(ByteBuf packet) {
        if (!enabled || packet.readableBytes() < 32) {
            return;
        }
        int size = Math.min(packet.readableBytes(), MAX_SAMPLE_BYTES);
        byte[] sample = new byte[size];
        packet.getBytes(packet.readerIndex(), sample);
        synchronized (LOCK) {
            if (!enabled || samples.size() >= MAX_SAMPLES || sampleBytes >= MAX_BUFFERED_BYTES) {
                return;
            }
            samples.add(sample);
            sampleBytes += size;
            if (samples.size() >= MIN_SAMPLES && sampleBytes >= MIN_SAMPLE_BYTES
                && !training && System.nanoTime() - lastTraining >= MIN_RETRAIN_NANOS) {
                scheduleTrainingLocked();
            }
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            generation++;
            current = new Snapshot(0, new byte[0]);
            samples = new ObjectArrayList<>();
            sampleBytes = 0;
            lastTraining = 0;
            training = false;
        }
    }

    private static void scheduleTrainingLocked() {
        ObjectList<byte[]> trainingSamples = samples;
        samples = new ObjectArrayList<>();
        sampleBytes = 0;
        training = true;
        long trainingGeneration = generation;
        TRAINER.execute(() -> train(trainingSamples, trainingGeneration));
    }

    private static void train(ObjectList<byte[]> trainingSamples, long trainingGeneration) {
        try {
            byte[][] corpus = trainingSamples.toArray(new byte[trainingSamples.size()][]);
            byte[] destination = new byte[MAX_DICTIONARY_BYTES];
            long result = Zstd.trainFromBuffer(corpus, destination, false);
            if (Zstd.isError(result) || result < MIN_PUBLISH_BYTES || result > MAX_DICTIONARY_BYTES) {
                LOG.fine("Unable to train bandwidth dictionary: "
                    + (Zstd.isError(result) ? Zstd.getErrorName(result) : result));
                return;
            }
            byte[] dictionary = new byte[(int) result];
            System.arraycopy(destination, 0, dictionary, 0, dictionary.length);
            Snapshot baseline;
            synchronized (LOCK) {
                if (trainingGeneration != generation || !enabled) {
                    return;
                }
                baseline = current;
            }
            long baselineSize = compressionScore(trainingSamples, baseline.bytes());
            long candidateSize = compressionScore(trainingSamples, dictionary);
            if (candidateSize * 100L > baselineSize * (100L - MIN_IMPROVEMENT_PERCENT)) {
                LOG.fine("Discarded bandwidth dictionary candidate (" + candidateSize
                    + " bytes vs " + baselineSize + " bytes baseline)");
                return;
            }
            synchronized (LOCK) {
                if (trainingGeneration != generation || !enabled) {
                    return;
                }
                if (current.version == Integer.MAX_VALUE) {
                    LOG.warning("Bandwidth dictionary version space exhausted; keeping the current dictionary");
                    return;
                }
                int nextVersion = current.version + 1;
                current = new Snapshot(nextVersion, dictionary);
                lastTraining = System.nanoTime();
                LOG.fine("Trained bandwidth dictionary version " + nextVersion + " (" + dictionary.length
                    + " bytes from " + trainingSamples.size() + " samples)");
            }
        } catch (Throwable failure) {
            LOG.log(Level.FINE, "Bandwidth dictionary training failed", failure);
        } finally {
            synchronized (LOCK) {
                // A reset or disable may happen while the daemon trainer is still
                // finishing native work. A stale task must not clear the state of a
                // newer generation or start a second trainer for it.
                if (trainingGeneration == generation) {
                    lastTraining = System.nanoTime();
                    training = false;
                    if (enabled && samples.size() >= MIN_SAMPLES
                        && sampleBytes >= MIN_SAMPLE_BYTES && System.nanoTime() - lastTraining >= MIN_RETRAIN_NANOS) {
                        scheduleTrainingLocked();
                    }
                }
            }
        }
    }

    private static long compressionScore(ObjectList<byte[]> samples, byte[] dictionary) {
        long total = 0;
        for (byte[] sample : samples) {
            byte[] compressed = dictionary.length == 0
                ? Zstd.compress(sample, 3)
                : Zstd.compressUsingDict(sample, dictionary, 3);
            total += compressed.length;
        }
        return total;
    }

    public record Snapshot(int version, byte[] bytes) {
        public Snapshot {
            if (version < 0 || bytes == null || bytes.length > MAX_DICTIONARY_BYTES
                || (bytes.length == 0 && version != 0) || (bytes.length != 0 && version == 0)) {
                throw new IllegalArgumentException("Invalid bandwidth dictionary snapshot");
            }
        }
    }
}
