package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirtyNotificationStateTest {
    @Test
    void skipsOnlyAnAlreadyCommittedVersionInTheSameTick() {
        final DirtyNotificationState state = new DirtyNotificationState();
        state.changed();
        final long first = state.capture();
        assertFalse(state.alreadyCommitted(first, 10));
        state.committed(first, 10);
        assertTrue(state.alreadyCommitted(first, 10));
        assertFalse(state.alreadyCommitted(first, 11));
        state.changed();
        assertFalse(state.alreadyCommitted(state.capture(), 10));
    }

    @Test
    void mutationDuringNotificationStillNeedsAnotherCommit() {
        final DirtyNotificationState state = new DirtyNotificationState();
        state.changed();
        final long captured = state.capture();
        state.changed();
        state.committed(captured, 10);
        assertFalse(state.alreadyCommitted(state.capture(), 10));
        state.committed(state.capture(), 10);
        state.committed(captured, 10);
        assertTrue(state.alreadyCommitted(state.capture(), 10));
    }

    @Test
    void queueScopeIsRestoredWhenSubclassCodeThrows() {
        final DirtyNotificationState state = new DirtyNotificationState();
        assertThrows(IllegalStateException.class, () -> {
            state.enterQueued();
            try {
                state.enterQueued();
                try {
                    assertTrue(state.isQueued());
                    throw new IllegalStateException();
                } finally {
                    state.leaveQueued();
                }
            } finally {
                state.leaveQueued();
            }
        });
        assertFalse(state.isQueued());
    }
}
