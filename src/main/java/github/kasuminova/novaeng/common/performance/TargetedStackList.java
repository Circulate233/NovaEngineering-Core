package github.kasuminova.novaeng.common.performance;

import com.valkyrieofnight.vliblegacy.lib.stack.WeightedItemStack;
import com.valkyrieofnight.vliblegacy.lib.stack.WeightedOreStack;
import com.valkyrieofnight.vliblegacy.lib.stack.WeightedStackBase;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A per-call item index for Environmental Tech's copied, ordered weight list.
 */
@SuppressWarnings("CloneableClassInSecureContext")
// Retain ArrayList's clone API; clone() discards the temporary index.
public final class TargetedStackList extends ArrayList<WeightedStackBase> {

    private Reference2IntOpenHashMap<Item> heads;
    private int[] next;
    private boolean disabled;

    public TargetedStackList(final int initialCapacity) {
        super(initialCapacity);
    }

    public Iterator<WeightedStackBase> matching(final ItemStack input, final int focusCount) {
        if (input == null || input.getClass() != ItemStack.class || input.isEmpty()) {
            finishBuilding();
            return iterator();
        }
        if (disabled || size() < 32 || focusCount < 8) {
            return iterator();
        }
        if (heads == null && !buildIndex()) {
            return iterator();
        }
        final int first = heads.getInt(input.getItem());
        return first < 0 ? Collections.emptyIterator() : new MatchingIterator(this, next, first);
    }

    private boolean buildIndex() {
        final Reference2IntOpenHashMap<Item> newHeads = new Reference2IntOpenHashMap<>(size());
        newHeads.defaultReturnValue(-1);
        final int[] newNext = new int[size()];
        // Inserting backwards makes each bucket visit its elements in the original list order.
        for (int index = size() - 1; index >= 0; index--) {
            final WeightedStackBase stack = get(index);
            if (stack == null || stack.getClass() != WeightedItemStack.class && stack.getClass() != WeightedOreStack.class) {
                disabled = true;
                return false;
            }
            final ItemStack main = stack.getMainStack();
            // isItemEqual uses the raw item; an empty stack's getItem() instead returns AIR.
            if (main == null || main.getClass() != ItemStack.class || main.isEmpty()) {
                disabled = true;
                return false;
            }
            newNext[index] = newHeads.put(main.getItem(), index);
        }
        if (newHeads.size() <= 1) {
            disabled = true;
            return false;
        }
        heads = newHeads;
        next = newNext;
        return true;
    }

    public void finishBuilding() {
        heads = null;
        next = null;
        disabled = true;
    }

    @Override
    public TargetedStackList clone() {
        final TargetedStackList copy = (TargetedStackList) super.clone();
        copy.finishBuilding();
        return copy;
    }

    private static final class MatchingIterator implements Iterator<WeightedStackBase> {
        private final TargetedStackList stacks;
        private final int[] next;
        private int cursor;

        private MatchingIterator(final TargetedStackList stacks, final int[] next, final int cursor) {
            this.stacks = stacks;
            this.next = next;
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor >= 0;
        }

        @Override
        public WeightedStackBase next() {
            if (cursor < 0) {
                throw new NoSuchElementException();
            }
            final WeightedStackBase result = stacks.get(cursor);
            cursor = next[cursor];
            return result;
        }
    }
}
