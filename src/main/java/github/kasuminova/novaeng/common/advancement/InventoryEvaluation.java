package github.kasuminova.novaeng.common.advancement;

import github.kasuminova.novaeng.common.performance.PerformanceMetrics;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.BiPredicate;

/**
 * A single flush's live stack view. Never caches results or stack contents across ticks.
 */
public final class InventoryEvaluation {
    private static final BiPredicate<ItemPredicate, ItemStack> TEST = ItemPredicate::test;
    private final IdentityStackIndex<Item, ItemStack> index = new IdentityStackIndex<>();
    private boolean inUse;
    private int full;
    private int empty;

    public boolean inUse() {
        return inUse;
    }

    public void prepare(final InventoryPlayer inventory, final boolean countFull) {
        if (inUse) {
            throw new IllegalStateException("Inventory view is already in use");
        }
        inUse = true;
        final int slots = inventory.getSizeInventory();
        PerformanceMetrics.add(PerformanceMetrics.Counter.INVENTORY_SLOTS, slots);
        for (int slot = 0; slot < slots; slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                empty++;
            } else {
                index.add(stack.getItem(), stack);
                if (countFull && stack.getCount() >= stack.getMaxStackSize()) {
                    full++;
                }
            }
        }
    }

    public boolean matches(final Item namedItem, final ItemPredicate predicate) {
        // A custom predicate can override the vanilla item restriction.
        final Item key = predicate.getClass() == ItemPredicate.class ? namedItem : null;
        if (key != null && !index.contains(key)) {
            PerformanceMetrics.add(PerformanceMetrics.Counter.INVENTORY_ITEM_MISSES, 1);
            return false;
        }
        return index.matches(key, predicate, TEST);
    }

    public int full() {
        return full;
    }

    public int empty() {
        return empty;
    }

    public int occupied() {
        return index.size();
    }

    public void clear() {
        index.clear();
        full = 0;
        empty = 0;
        inUse = false;
    }
}
