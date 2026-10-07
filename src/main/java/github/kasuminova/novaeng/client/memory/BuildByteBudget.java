package github.kasuminova.novaeng.client.memory;

/** Byte backpressure accounts for queued/running reservations and completed outputs separately. */
public final class BuildByteBudget {
    private long reserved;
    private long completed;
    private long estimate = 8L * 1024 * 1024;
    private int inFlight;

    public synchronized boolean canSchedule(final long limit) {
        final long total = reserved + completed;
        // One oversized job must still be able to make progress when the pipeline is empty.
        return total == 0 || estimate <= limit - total;
    }

    /** Admission is tested before snapshot creation. Urgent work is allowed to exceed the soft limit. */
    public synchronized long reserve() {
        final long amount = estimate;
        reserved = Math.addExact(reserved, amount);
        inFlight++;
        return amount;
    }

    public synchronized void complete(final long reservation, final long outputBytes) {
        if (reservation > reserved || inFlight <= 0 || outputBytes < 0) {
            throw new IllegalStateException("Unbalanced chunk-build reservation");
        }
        reserved -= reservation;
        inFlight--;
        completed = Math.addExact(completed, outputBytes);
        if (outputBytes > 0) {
            // Rise immediately for an outlier and decay gradually after the workload gets smaller.
            estimate = Math.max(64 * 1024L, Math.max(outputBytes, estimate - (estimate - outputBytes) / 8));
        }
    }

    public synchronized void releaseOutput(final long bytes) {
        if (bytes < 0 || bytes > completed) {
            throw new IllegalStateException("Unbalanced completed chunk output");
        }
        completed -= bytes;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(reserved, completed, inFlight, estimate);
    }

    public record Snapshot(long reservedBytes, long completedBytes, int inFlight, long estimatedJobBytes) { }
}
