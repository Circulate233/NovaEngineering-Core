package github.kasuminova.novaeng.common.performance;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionTickIdsTest {

    private static void referenceCheck(final Int2ObjectMap<TestWorld> worlds, final Map<TestWorld, ?> weak,
                                       final Multiset<Integer> counts, final List<Report> reports) {
        final List<TestWorld> unloaded = new ArrayList<>(weak.keySet());
        unloaded.removeAll(worlds.values());
        for (final TestWorld world : unloaded) {
            counts.add(System.identityHashCode(world));
        }
        for (final TestWorld world : unloaded) {
            final int count = counts.count(System.identityHashCode(world));
            if (count == 5 || count % 5 == 0) {
                reports.add(new Report(world, count));
            }
        }
    }

    private static TestWorld world(final int id, final Int2IntOpenHashMap counts, final List<Report> reports) throws Exception {
        final Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        final TestWorld world = (TestWorld) ((Unsafe) field.get(null)).allocateInstance(TestWorld.class);
        world.id = id;
        world.counts = counts;
        world.reports = reports;
        return world;
    }

    @Test
    void primitiveCountsAndReportsMatchForgeAcrossWorldLifecycleChanges() throws Exception {
        final Int2IntOpenHashMap actualCounts = new Int2IntOpenHashMap();
        final List<Report> actual = new ObjectArrayList<>();
        final TestWorld first = world(1, actualCounts, actual);
        final TestWorld second = world(2, actualCounts, actual);
        final Int2ObjectMap<TestWorld> worlds = new Int2ObjectLinkedOpenHashMap<>();
        final Map<TestWorld, TestWorld> weak = new Reference2ReferenceOpenHashMap<>();
        weak.put(first, first);
        weak.put(second, second);
        worlds.put(0, first);
        final Multiset<Integer> expectedCounts = HashMultiset.create();
        final List<Report> expected = new ObjectArrayList<>();
        for (int check = 1; check <= 20; check++) {
            if (check == 3) {
                worlds.remove(0);
            } else if (check == 7) {
                worlds.put(-1, second);
            } else if (check == 9) {
                worlds.remove(-1);
            } else if (check == 12) {
                weak.remove(first);
            }
            referenceCheck(worlds, weak, expectedCounts, expected);
            DimensionTickIds.checkLeaks(worlds, weak, actualCounts);
            assertEquals(expectedCounts.count(System.identityHashCode(first)), actualCounts.get(System.identityHashCode(first)));
            assertEquals(expectedCounts.count(System.identityHashCode(second)), actualCounts.get(System.identityHashCode(second)));
            assertEquals(expected, actual, "check " + check);
        }
        assertTrue(actual.stream().anyMatch(report -> report.count() == 5));
        assertTrue(actual.stream().anyMatch(report -> report.count() == 10));
        assertTrue(actual.stream().allMatch(report -> report.count() % 5 == 0));
        assertEquals(9, actualCounts.get(System.identityHashCode(first)));
    }

    @Test
    void loadedMembershipRetainsEqualsRatherThanReferenceSemantics() throws Exception {
        final Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        final List<Report> reports = new ObjectArrayList<>();
        final TestWorld loaded = world(1, counts, reports);
        final TestWorld equivalent = world(1, counts, reports);
        assertNotSame(loaded, equivalent);
        final Int2ObjectMap<TestWorld> worlds = new Int2ObjectLinkedOpenHashMap<>();
        worlds.put(0, loaded);
        final Map<TestWorld, TestWorld> weak = new Reference2ReferenceOpenHashMap<>();
        weak.put(equivalent, equivalent);
        DimensionTickIds.checkLeaks(worlds, weak, counts);
        assertTrue(counts.isEmpty());
        assertTrue(reports.isEmpty());
    }

    @Test
    void identityHashCollisionsAreCountedBeforeEitherReport() throws Exception {
        final Int2ObjectMap<TestWorld> hashes = new Int2ObjectOpenHashMap<>();
        final Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        final List<Report> reports = new ObjectArrayList<>();
        TestWorld first = null;
        TestWorld second = null;
        for (int i = 0; i < 400_000; i++) {
            final TestWorld candidate = world(i, counts, reports);
            final TestWorld previous = hashes.putIfAbsent(System.identityHashCode(candidate), candidate);
            if (previous != null) {
                first = previous;
                second = candidate;
                break;
            }
        }
        assertNotNull(first, "No identity-hash collision found within the test bound");
        final Map<TestWorld, TestWorld> weak = new Reference2ReferenceOpenHashMap<>();
        weak.put(first, first);
        weak.put(second, second);
        final int hash = System.identityHashCode(first);
        counts.put(hash, 3);
        DimensionTickIds.checkLeaks(new Int2ObjectLinkedOpenHashMap<>(), weak, counts);
        assertEquals(5, counts.get(hash));
        assertEquals(2, reports.size());
        assertTrue(reports.stream().allMatch(report -> report.count() == 5));
    }

    @Test
    void countOverflowRetainsGuavaFailureSemantics() throws Exception {
        final Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        final TestWorld leaked = world(1, counts, new ObjectArrayList<>());
        final Map<TestWorld, TestWorld> weak = new Reference2ReferenceOpenHashMap<>();
        weak.put(leaked, leaked);
        final int hash = System.identityHashCode(leaked);
        counts.put(hash, Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,
            () -> DimensionTickIds.checkLeaks(new Int2ObjectLinkedOpenHashMap<>(), weak, counts));
        assertEquals(Integer.MAX_VALUE, counts.get(hash));
    }

    @Test
    void tickEntryNeverCallsBoxedIdsOrBoxedLeakCounters() throws IOException {
        final ClassNode node = new ClassNode();
        try (final InputStream input = DimensionTickIdsTest.class.getResourceAsStream("DimensionTickIds.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        boolean primitiveCopy = false;
        for (final MethodNode method : node.methods) {
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner.equals("net/minecraftforge/common/DimensionManager") && call.name.equals("getIDs"));
                    if (method.name.equals("get")) {
                        assertFalse(call.owner.equals("java/lang/Integer") && call.name.equals("valueOf"));
                    }
                    assertNotEquals("com/google/common/collect/Multiset", call.owner);
                    if (call.owner.equals("it/unimi/dsi/fastutil/ints/Int2IntOpenHashMap")
                        && (call.name.equals("get") || call.name.equals("addTo"))) {
                        assertTrue(call.desc.equals("(I)I") || call.desc.equals("(II)I"));
                    }
                    primitiveCopy |= call.name.equals("toIntArray") && call.desc.equals("()[I");
                }
                if (instruction instanceof TypeInsnNode type) {
                    assertNotEquals("java/lang/Integer", type.desc);
                }
            }
        }
        assertTrue(primitiveCopy);
    }

    private record Report(Object world, int count) {
    }

    private static final class TestWorld extends WorldServer {
        private static final WorldInfo INFO = new TestWorldInfo();
        private int id;
        private Int2IntOpenHashMap counts;
        private List<Report> reports;

        private TestWorld() {
            super(null, null, null, 0, null);
            throw new AssertionError("The test only needs world identity, not a running server");
        }

        @Override
        public WorldInfo getWorldInfo() {
            this.reports.add(new Report(this, this.counts.get(System.identityHashCode(this))));
            return INFO;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof TestWorld world && world.id == this.id;
        }

        @Override
        public int hashCode() {
            return this.id;
        }
    }

    private static final class TestWorldInfo extends WorldInfo {
        @Override
        public String getWorldName() {
            return "test";
        }
    }
}
