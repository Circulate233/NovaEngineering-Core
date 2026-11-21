package github.kasuminova.novaeng.common.performance.ae2;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import github.kasuminova.novaeng.common.performance.AvailableStackIteration;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingSnapshotCopyTest {
    @Test
    void capacityUsesTheOuterItemCountAndRemainsBoundedPerSnapshot() {
        final Source variants = new Source(new Stack(5, 0, false).stack, new Stack(7, 0, false).stack) {
            @Override
            public int nova$itemTypeCount() {
                return 1;
            }
        };
        final OwnedList destination = new OwnedList();
        CraftingSnapshotCopy.copyOrOriginal(variants, destination);
        assertEquals(1, destination.reserved, "two variants belong to one outer Item bucket");
        assertEquals(2, destination.size());
        final Source large = new Source() {
            @Override
            public int nova$itemTypeCount() {
                return Integer.MAX_VALUE;
            }
        };
        final OwnedList bounded = new OwnedList();
        CraftingSnapshotCopy.copyOrOriginal(large, bounded);
        assertEquals(4096, bounded.reserved);
    }

    @Test
    void bulkCopyRetainsOwnedStacksFlagsAndLongCountsWithoutReadingSizeOrIterator() {
        final Stack first = new Stack(1L << 42, 17, true);
        final Stack second = new Stack(0, Long.MAX_VALUE, false);
        final Source source = new Source(first.stack, second.stack);
        final OwnedList firstSnapshot = new OwnedList();
        final IItemList<IAEItemStack> consumed = CraftingSnapshotCopy.copyOrOriginal(source, firstSnapshot);

        assertEquals(0, consumed.size(), "RC must not reserve using the source variant count");
        assertFalse(consumed.iterator().hasNext());
        assertEquals(2, firstSnapshot.size());
        assertEquals(1L << 42, firstSnapshot.records.get(0).getStackSize());
        assertEquals(17, firstSnapshot.records.get(0).getCountRequestable());
        assertEquals(Long.MAX_VALUE, firstSnapshot.records.get(1).getCountRequestable());
        assertFalse(firstSnapshot.records.get(0).isCraftable());
        assertTrue(first.stack.isCraftable(), "copying must not clear the monitor's craftable flag");
        assertNotSame(first.stack, firstSnapshot.records.get(0));
        assertEquals(1, first.copies, "the owned snapshot copy is required");

        final OwnedList secondSnapshot = new OwnedList();
        CraftingSnapshotCopy.copyOrOriginal(source, secondSnapshot);
        firstSnapshot.records.get(0).setStackSize(3);
        assertEquals(1L << 42, first.stack.getStackSize());
        assertEquals(1L << 42, secondSnapshot.records.get(0).getStackSize());
        assertEquals(2, first.copies, "each job owns a fresh snapshot");
        assertEquals(0, source.sizeReads);
        assertEquals(0, source.iteratorReads);
    }

    @Test
    void emptySnapshotsAndConcurrentJobsDoNotShareMutableState() throws Exception {
        final Source empty = new Source();
        final OwnedList emptyCopy = new OwnedList();
        assertTrue(CraftingSnapshotCopy.copyOrOriginal(empty, emptyCopy).isEmpty());
        assertTrue(emptyCopy.isEmpty());
        final var failures = new ConcurrentLinkedQueue<Throwable>();
        final Thread[] jobs = new Thread[3];
        for (int i = 0; i < jobs.length; i++) {
            final int count = i + 1;
            jobs[i] = new Thread(() -> {
                try {
                    for (int attempt = 0; attempt < 100; attempt++) {
                        final Stack stack = new Stack(count, count * 1000L, true);
                        final OwnedList destination = new OwnedList();
                        final IItemList<IAEItemStack> consumed = CraftingSnapshotCopy.copyOrOriginal(new Source(stack.stack), destination);
                        assertEquals(count, destination.getFirstItem().getStackSize());
                        assertEquals(0, consumed.size());
                        assertEquals(1, destination.size());
                    }
                } catch (final Throwable failure) {
                    failures.add(failure);
                }
            });
            jobs[i].start();
        }
        for (final Thread job : jobs) job.join();
        assertTrue(failures.isEmpty(), failures.toString());
    }

    @Test
    void unknownSourcesAndNullRetainTheOriginalConstructorPath() {
        final OwnedList source = new OwnedList();
        final OwnedList destination = new OwnedList();
        assertSame(source, CraftingSnapshotCopy.copyOrOriginal(source, destination));
        assertNull(CraftingSnapshotCopy.copyOrOriginal(null, destination));
        assertTrue(destination.isEmpty());
    }

    @Test
    void copyFailuresAndConcurrentModificationArePropagated() {
        final Source source = new Source(new Stack(3, 0, false).stack) {
            @Override
            public void nova$forEachAvailable(final Consumer<? super IAEItemStack> consumer) {
                super.nova$forEachAvailable(consumer);
                throw new ConcurrentModificationException();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> CraftingSnapshotCopy.copyOrOriginal(source, new OwnedList()));
        final OwnedList rejected = new OwnedList() {
            @Override
            public void add(final IAEItemStack stack) {
                throw new IllegalStateException("copy failed");
            }
        };
        assertThrows(IllegalStateException.class,
            () -> CraftingSnapshotCopy.copyOrOriginal(new Source(new Stack(1, 0, false).stack), rejected));
    }

    // Only quantities and flags are involved in the copy helper. Keep item definitions out
    // of this test so it can run without bootstrapping Minecraft or a running client.
    private static final class Stack {
        private final long requestable;
        private final IAEItemStack stack;
        private long count;
        private boolean craftable;
        private int copies;

        private Stack(final long count, final long requestable, final boolean craftable) {
            this.count = count;
            this.requestable = requestable;
            this.craftable = craftable;
            stack = (IAEItemStack) Proxy.newProxyInstance(IAEItemStack.class.getClassLoader(), new Class<?>[]{IAEItemStack.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStackSize" -> this.count;
                    case "getCountRequestable" -> this.requestable;
                    case "isCraftable" -> this.craftable;
                    case "isMeaningful" -> this.count != 0 || this.requestable != 0 || this.craftable;
                    case "setStackSize" -> {
                        this.count = (long) args[0];
                        yield proxy;
                    }
                    case "setCraftable" -> {
                        this.craftable = (boolean) args[0];
                        yield proxy;
                    }
                    case "copy" -> {
                        this.copies++;
                        yield new Stack(this.count, this.requestable, this.craftable).stack;
                    }
                    case "toString" -> "stack:" + this.count;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        }
    }

    private static class OwnedList implements IItemList<IAEItemStack>, CraftingSnapshotCapacity {
        final ObjectArrayList<IAEItemStack> records = new ObjectArrayList<>();
        private int reserved = -1;

        @Override
        public int nova$itemTypeCount() {
            return records.size();
        }

        @Override
        public void nova$reserveItemTypes(final int count) {
            assertTrue(records.isEmpty());
            reserved = count;
        }

        @Override
        public void add(final IAEItemStack stack) {
            records.add(stack.copy().setCraftable(false));
        }

        @Override
        public void addStorage(final IAEItemStack stack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCrafting(final IAEItemStack stack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addRequestable(final IAEItemStack stack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IAEItemStack findPrecise(final IAEItemStack stack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<IAEItemStack> findFuzzy(final IAEItemStack stack, final FuzzyMode mode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return records.isEmpty();
        }

        @Override
        public IAEItemStack getFirstItem() {
            return records.isEmpty() ? null : records.getFirst();
        }

        @Override
        public int size() {
            return records.size();
        }

        @Override
        public Iterator<IAEItemStack> iterator() {
            return records.iterator();
        }

        @Override
        public void resetStatus() {
            throw new UnsupportedOperationException();
        }
    }

    private static class Source extends OwnedList implements AvailableStackIteration<IAEItemStack> {
        private int sizeReads;
        private int iteratorReads;

        private Source(final IAEItemStack... stacks) {
            records.addAll(List.of(stacks));
        }

        @Override
        public void nova$forEachAvailable(final Consumer<? super IAEItemStack> consumer) {
            for (int i = 0; i < records.size(); i++) consumer.accept(records.get(i));
        }

        @Override
        public int size() {
            sizeReads++;
            throw new AssertionError("source.size allocates per-variant iterators");
        }

        @Override
        public Iterator<IAEItemStack> iterator() {
            iteratorReads++;
            throw new AssertionError("use the existing bulk visitor");
        }
    }
}
