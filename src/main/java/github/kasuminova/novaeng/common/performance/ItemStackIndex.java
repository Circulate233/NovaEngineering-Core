package github.kasuminova.novaeng.common.performance;

import hellfirepvp.modularmachinery.common.util.ItemUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.List;

/**
 * Append-only, operation-local index; source stacks must not mutate before close.
 */
public final class ItemStackIndex implements AutoCloseable {

    public static final int INDEX_THRESHOLD = 32;
    private static final int MAX_RETAINED_SIZE = 4096;
    private static final ThreadLocal<ItemStackIndex> IDLE = ThreadLocal.withInitial(ItemStackIndex::new);
    private List<ItemStack> stacks;
    private boolean matchCount;
    private Int2IntOpenHashMap buckets;
    private int[] previous;
    private int indexedSize;
    private boolean linear;

    private ItemStackIndex() {
    }

    public static ItemStackIndex open(final List<ItemStack> stacks, final boolean matchCount) {
        ItemStackIndex index = IDLE.get();
        if (index.stacks != null) {
            index = new ItemStackIndex();
        }
        index.stacks = stacks;
        index.matchCount = matchCount;
        return index;
    }

    private static int findLinear(final List<ItemStack> stacks, final ItemStack stack, final boolean matchCount) {
        if (matchCount) {
            for (int i = 0; i < stacks.size(); i++) {
                final ItemStack existing = stacks.get(i);
                if (ItemUtils.matchStacks(stack, existing) && stack.getCount() == existing.getCount()) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < stacks.size(); i++) {
                if (ItemUtils.matchStacks(stack, stacks.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int find(final ItemStack stack) {
        if (this.linear || this.indexedSize == 0 && this.stacks.size() < INDEX_THRESHOLD) {
            return this.findLinear(stack);
        }
        if (this.buckets == null) {
            this.buckets = new Int2IntOpenHashMap(INDEX_THRESHOLD);
            this.buckets.defaultReturnValue(-1);
            this.previous = new int[INDEX_THRESHOLD];
        }
        if (this.previous.length < this.stacks.size()) {
            this.previous = Arrays.copyOf(this.previous,
                Math.max(this.stacks.size(), this.previous.length * 2));
        }
        while (this.indexedSize < this.stacks.size()) {
            final int index = this.indexedSize++;
            this.previous[index] = this.buckets.put(this.hash(this.stacks.get(index)), index);
        }
        // A coarse key cannot separate many variants of one item; keep the original scan in that case.
        if (this.buckets.size() * 8 < this.indexedSize) {
            this.linear = true;
            return this.findLinear(stack);
        }
        int first = -1;
        for (int index = this.buckets.get(this.hash(stack)); index >= 0; index = this.previous[index]) {
            final ItemStack existing = this.stacks.get(index);
            if (ItemUtils.matchStacks(stack, existing)
                && (!this.matchCount || stack.getCount() == existing.getCount())) {
                first = index;
            }
        }
        return first;
    }

    public int findOrAdd(final ItemStack stack) {
        final int existing = this.find(stack);
        if (existing >= 0) {
            return existing;
        }
        final int index = this.stacks.size();
        this.stacks.add(stack);
        return index;
    }

    private int findLinear(final ItemStack stack) {
        return findLinear(this.stacks, stack, this.matchCount);
    }

    private int hash(final ItemStack stack) {
        // Damage accessors can be overridden; NBT.hashCode allocates an iterator for every compound.
        final NBTTagCompound tag = stack.getTagCompound();
        int hash = stack.isEmpty() ? 0 : 31 * System.identityHashCode(stack.getItem())
            + (tag == null ? 0 : tag.getSize() + 1);
        if (this.matchCount) {
            hash = 31 * hash + stack.getCount();
        }
        return hash;
    }

    @Override
    public void close() {
        this.stacks = null;
        this.indexedSize = 0;
        this.linear = false;
        if (this.buckets != null) {
            if (this.previous.length > MAX_RETAINED_SIZE) {
                this.buckets = null;
                this.previous = null;
            } else {
                this.buckets.clear();
            }
        }
    }
}
