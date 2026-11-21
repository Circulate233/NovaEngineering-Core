package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class NarrowSnapshotTest {
    @Test
    void keepsMatchingReferencesOrderDuplicatesAndSnapshotIsolation() {
        final Object first = new StringBuilder("first"), second = new StringBuilder("second");
        final ObjectArrayList<Object> source = new ObjectArrayList<>();
        source.add(1);
        source.add(first);
        source.add(null);
        source.add(second);
        source.add(first);
        final ArrayList<Object> selected = NarrowSnapshot.arrayListOfType(source, StringBuilder.class);
        assertEquals(List.of(first, second, first), selected);
        assertSame(first, selected.iterator().next());
        source.clear();
        assertEquals(3, selected.size());
        ((StringBuilder) first).append(" changed");
        assertEquals("first changed", selected.iterator().next().toString());
    }

    @Test
    void emptyFinalSnapshotsRemainIndependentAndCustomCollectionsKeepTheirBoundary() {
        final ObjectArrayList<Object> ordinary = new ObjectArrayList<>();
        for (int i = 0; i < 10000; i++) ordinary.add(i);
        final ArrayList<Object> first = NarrowSnapshot.arrayListOfType(ordinary, StringBuilder.class);
        final ArrayList<Object> second = NarrowSnapshot.arrayListOfType(ordinary, StringBuilder.class);
        assertNotSame(first, second);
        first.add("writable final snapshot");
        assertEquals(0, second.size());
        final List<Object> custom = new AbstractList<>() {
            public Object get(final int index) {
                throw new AssertionError("Must keep the original collection path");
            }

            public int size() {
                return 1;
            }
        };
        assertNull(NarrowSnapshot.arrayListOfType(custom, StringBuilder.class));
    }
}
