package github.kasuminova.novaeng.common.performance.ae2;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import github.kasuminova.novaeng.common.performance.AvailableStackIteration;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.Collection;
import java.util.Iterator;

/**
 * Copies into the inventory's own list, without an iterator for every item variant.
 */
public final class CraftingSnapshotCopy {
    private static final IItemList<IAEItemStack> CONSUMED = new ConsumedSource();

    private CraftingSnapshotCopy() {
    }

    /**
     * Called at the source argument's only load, after localCache has been initialized.
     * Returning an empty source leaves the original constructor loop intact. In particular,
     * RandomComplement's iterator redirect now sees size zero and does not reserve another
     * outer item map using the number of variants. Unknown source implementations retain
     * their original iterator and all its behavior.
     */
    @SuppressWarnings("unchecked")
    public static IItemList<IAEItemStack> copyOrOriginal(final IItemList<IAEItemStack> source,
                                                         final IItemList<IAEItemStack> destination) {
        if (!(source instanceof AvailableStackIteration<?> iteration)) {
            return source;
        }
        if (source instanceof CraftingSnapshotCapacity hint && destination instanceof CraftingSnapshotCapacity capacity) {
            // Preserve bounded, per-source sizing without scanning all variants or
            // sizing an outer Item map from the much larger variant count.
            capacity.nova$reserveItemTypes(Math.min(hint.nova$itemTypeCount(), 4096));
        }
        ((AvailableStackIteration<IAEItemStack>) iteration).nova$forEachAvailable(destination::add);
        return CONSUMED;
    }

    // This sentinel is only read by the remainder of the constructor. Never retain the
    // source, destination or any stack here; snapshots must remain independent across jobs.
    private static final class ConsumedSource implements IItemList<IAEItemStack> {
        @Override
        public void add(final IAEItemStack option) {
            throw new UnsupportedOperationException("Consumed crafting snapshot source");
        }

        @Override
        public void addStorage(final IAEItemStack option) {
            throw new UnsupportedOperationException("Consumed crafting snapshot source");
        }

        @Override
        public void addCrafting(final IAEItemStack option) {
            throw new UnsupportedOperationException("Consumed crafting snapshot source");
        }

        @Override
        public void addRequestable(final IAEItemStack option) {
            throw new UnsupportedOperationException("Consumed crafting snapshot source");
        }

        @Override
        public IAEItemStack findPrecise(final IAEItemStack filter) {
            return null;
        }

        @Override
        public Collection<IAEItemStack> findFuzzy(final IAEItemStack filter, final FuzzyMode fuzzy) {
            return ObjectLists.emptyList();
        }

        @Override
        public IAEItemStack getFirstItem() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<IAEItemStack> iterator() {
            return ObjectIterators.emptyIterator();
        }

        @Override
        public void resetStatus() {
            throw new UnsupportedOperationException("Consumed crafting snapshot source");
        }
    }
}
