package github.kasuminova.novaeng.common.advancement;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerInstanceSnapshotTest {

    /** Stands in for a listener: an element whose derived value is fixed at construction. */
    private static final class Element {

        private final String name;
        private final String value;

        private Element(final String name) {
            this.name = name;
            this.value = name + "-instance";
        }
    }

    private static final Function<Element, String> VALUE_OF = element -> element.value;

    private final Set<Element> source = new LinkedHashSet<>();

    @Test
    void readsTheCollectionInItsOwnOrder() {
        this.source.add(new Element("a"));
        this.source.add(new Element("b"));
        this.source.add(new Element("c"));

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, VALUE_OF);

        assertEquals(3, snapshot.size());
        assertEquals("a", snapshot.listener(0).name);
        assertEquals("b", snapshot.listener(1).name);
        assertEquals("c", snapshot.listener(2).name);
        assertEquals("a-instance", snapshot.instance(0));
        assertEquals("b-instance", snapshot.instance(1));
        assertEquals("c-instance", snapshot.instance(2));
    }

    @Test
    void derivesEachValueExactlyOncePerBuild() {
        this.source.add(new Element("a"));
        this.source.add(new Element("b"));

        final List<String> reads = new ArrayList<>();
        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, element -> {
            reads.add(element.name);
            return element.value;
        });

        assertEquals(List.of("a", "b"), reads);

        // Reading the snapshot afterwards must not derive anything again - that is the whole point.
        snapshot.instance(0);
        snapshot.instance(1);
        assertEquals(List.of("a", "b"), reads);
    }

    @Test
    void rebuildAfterInvalidateSeesTheChangedCollection() {
        final Element a = new Element("a");
        final Element b = new Element("b");
        this.source.add(a);

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, VALUE_OF);
        assertEquals(1, snapshot.size());

        this.source.add(b);
        snapshot.invalidate();
        snapshot.rebuild(this.source, VALUE_OF);

        assertEquals(2, snapshot.size());
        assertEquals("a", snapshot.listener(0).name);
        assertEquals("b", snapshot.listener(1).name);
    }

    @Test
    void rebuildAfterRemovalDropsTheRemovedElement() {
        final Element a = new Element("a");
        final Element b = new Element("b");
        this.source.add(a);
        this.source.add(b);

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, VALUE_OF);

        this.source.remove(a);
        snapshot.invalidate();
        snapshot.rebuild(this.source, VALUE_OF);

        assertEquals(1, snapshot.size());
        assertEquals("b", snapshot.listener(0).name);
    }

    @Test
    void invalidateLeavesNoSnapshotHeld() {
        this.source.add(new Element("a"));

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        assertFalse(snapshot.isHeld());
        snapshot.rebuild(this.source, VALUE_OF);
        assertTrue(snapshot.isHeld());
        snapshot.invalidate();
        assertFalse(snapshot.isHeld());
    }

    @Test
    void emptyCollectionStillBuildsSoTheNextWalkDoesNotRebuild() {
        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, VALUE_OF);

        assertTrue(snapshot.isHeld());
        assertEquals(0, snapshot.size());
    }

    @Test
    void changingTheCollectionWithoutInvalidateKeepsTheStaleSnapshot() {
        // Documents the one case the class cannot detect: the caller owns invalidation. The mixin calls invalidate
        // from add/remove for exactly this reason.
        final Element a = new Element("a");
        this.source.add(a);

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, VALUE_OF);

        this.source.add(new Element("b"));

        assertEquals(1, snapshot.size());
        assertEquals("a", snapshot.listener(0).name);
    }

    @Test
    void nullDerivedValueIsKeptAsIs() {
        final Element a = new Element("a");
        this.source.add(a);

        final ListenerInstanceSnapshot<Element, String> snapshot = new ListenerInstanceSnapshot<>();
        snapshot.rebuild(this.source, element -> null);

        assertEquals(1, snapshot.size());
        assertNull(snapshot.instance(0));
        assertEquals("a", snapshot.listener(0).name);
    }
}