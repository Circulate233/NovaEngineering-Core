package github.kasuminova.novaeng.client.sound;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SoundUpdateBatchTest {
    @Test
    void notifiesOnceAfterAllThreeCommandsAreQueuedInOrder() {
        final ConsumerThread consumer = new ConsumerThread();
        SoundUpdateBatch.begin();
        try {
            consumer.queued.add("volume");
            SoundUpdateBatch.defer(consumer);
            consumer.queued.add("pitch");
            SoundUpdateBatch.defer(consumer);
            assertEquals(0, consumer.wakeups);
            consumer.queued.add("position");
            SoundUpdateBatch.finish(consumer);
            assertEquals(List.of("volume", "pitch", "position"), consumer.consumed);
        } finally {
            SoundUpdateBatch.end();
        }
        assertEquals(1, consumer.wakeups);
    }

    @Test
    void settersOutsideMinecraftUpdateKeepImmediateNotification() {
        final ConsumerThread consumer = new ConsumerThread();
        SoundUpdateBatch.defer(consumer);
        SoundUpdateBatch.defer(consumer);
        SoundUpdateBatch.finish(consumer);
        assertEquals(3, consumer.wakeups);
    }

    @Test
    void partialUpdateIsFlushedOnExceptionalExit() {
        final ConsumerThread consumer = new ConsumerThread();
        assertThrows(IllegalStateException.class, () -> {
            SoundUpdateBatch.begin();
            try {
                consumer.queued.add("volume");
                SoundUpdateBatch.defer(consumer);
                throw new IllegalStateException("sound position getter failed");
            } finally {
                SoundUpdateBatch.end();
            }
        });
        assertEquals(List.of("volume"), consumer.consumed);
        SoundUpdateBatch.defer(consumer);
        assertEquals(2, consumer.wakeups);
    }

    @Test
    void nestedUpdatesAndDifferentConsumersDoNotLoseNotifications() {
        final ConsumerThread first = new ConsumerThread();
        final ConsumerThread second = new ConsumerThread();
        SoundUpdateBatch.begin();
        try {
            SoundUpdateBatch.defer(first);
            SoundUpdateBatch.begin();
            try {
                assertEquals(1, first.wakeups);
                SoundUpdateBatch.defer(second);
            } finally {
                SoundUpdateBatch.end();
            }
            assertEquals(1, second.wakeups);
            SoundUpdateBatch.defer(first);
            SoundUpdateBatch.finish(second);
        } finally {
            SoundUpdateBatch.end();
        }
        assertEquals(2, first.wakeups);
        assertEquals(2, second.wakeups);
    }

    private static final class ConsumerThread extends Thread {
        private final List<String> queued = new ObjectArrayList<>();
        private final List<String> consumed = new ObjectArrayList<>();
        private int wakeups;

        @Override
        public void interrupt() {
            wakeups++;
            consumed.addAll(queued);
            queued.clear();
        }
    }
}
