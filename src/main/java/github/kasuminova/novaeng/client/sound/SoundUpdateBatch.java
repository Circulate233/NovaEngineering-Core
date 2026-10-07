package github.kasuminova.novaeng.client.sound;

/**
 * Coalesces the wakeups of Minecraft's consecutive volume, pitch and position commands.
 */
public final class SoundUpdateBatch {
    private static final ThreadLocal<State> STATES = ThreadLocal.withInitial(State::new);

    private SoundUpdateBatch() {
    }

    public static void begin() {
        final State state = STATES.get();
        state.flush();
        state.depth++;
    }

    public static void end() {
        final State state = STATES.get();
        try {
            // Also wakes the consumer if a sound getter or later setter threw an exception.
            state.flush();
        } finally {
            state.depth--;
        }
    }

    /**
     * The commands have already entered Paulscode's synchronized queue; only notification waits.
     */
    public static void defer(final Thread commandThread) {
        final State state = STATES.get();
        if (state.depth == 0) {
            commandThread.interrupt();
            return;
        }
        if (state.pending != commandThread) {
            state.flush();
            state.pending = commandThread;
        }
    }

    /**
     * Position is the last command in one tickable sound's update, before any playing() query.
     */
    public static void finish(final Thread commandThread) {
        final State state = STATES.get();
        if (state.pending == commandThread) {
            state.pending = null;
        } else {
            state.flush();
        }
        commandThread.interrupt();
    }

    private static final class State {
        private int depth;
        private Thread pending;

        private void flush() {
            final Thread thread = pending;
            pending = null;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}
