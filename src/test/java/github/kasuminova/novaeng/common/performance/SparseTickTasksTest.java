package github.kasuminova.novaeng.common.performance;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SparseTickTasksTest {

    @Test
    void absentWorldDoesNotCreateAnEntry() {
        final Multimap<String, Integer> tasks = LinkedHashMultimap.create();
        assertNull(SparseTickTasks.existing(tasks, "world"));
        assertEquals(0, tasks.size());
    }

    @Test
    void existingViewUpdatesMultimapSizeAndRemovesEmptyKeys() {
        final Multimap<String, Integer> tasks = LinkedHashMultimap.create();
        tasks.putAll("world", List.of(3, 1, 2));
        final Collection<Integer> view = SparseTickTasks.existing(tasks, "world");
        assertEquals(List.of(3, 1, 2), new ArrayList<>(view));
        view.removeIf(value -> value != 1);
        assertEquals(1, tasks.size());
        view.clear();
        assertNull(SparseTickTasks.existing(tasks, "world"));
        assertEquals(0, tasks.keySet().size());
    }

    @Test
    void pendingWorkMergesAllWorldsOnlyAtCommitAndPreservesUniqueness() {
        final Multimap<String, Integer> tasks = LinkedHashMultimap.create();
        final Multimap<String, Integer> pending = LinkedHashMultimap.create();
        tasks.put("a", 1);
        pending.putAll("a", List.of(1, 2));
        pending.put("b", 3);
        assertEquals(List.of(1), new ArrayList<>(SparseTickTasks.existing(tasks, "a")));
        assertNull(SparseTickTasks.existing(tasks, "b"));
        SparseTickTasks.mergePending(tasks, pending);
        assertEquals(List.of(1, 2), new ArrayList<>(tasks.get("a")));
        assertEquals(List.of(3), new ArrayList<>(tasks.get("b")));
        assertEquals(0, pending.size());
        SparseTickTasks.mergePending(tasks, pending);
        assertEquals(3, tasks.size());
    }
}
