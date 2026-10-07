package github.kasuminova.novaeng.common.performance;

/**
 * Per-tile change generations. Async producers only enter the short changed/capture critical sections.
 */
public final class DirtyNotificationState {
    private long mutation;
    private long committed = -1;
    private long committedTick = Long.MIN_VALUE;
    private int queuedDepth;

    public synchronized void changed() {
        mutation++;
    }

    public synchronized long capture() {
        return mutation;
    }

    public synchronized boolean alreadyCommitted(final long version, final long tick) {
        return committed == version && committedTick == tick;
    }

    public synchronized void committed(final long version, final long tick) {
        // A nested notification may already have committed a newer mutation.
        if (committedTick != tick || committed <= version) {
            committed = version;
            committedTick = tick;
        }
    }

    public void enterQueued() {
        queuedDepth++;
    }

    public void leaveQueued() {
        queuedDepth--;
    }

    public boolean isQueued() {
        return queuedDepth > 0;
    }
}
