package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.advancement.IndexedInventoryCriterion;
import github.kasuminova.novaeng.common.advancement.InventoryEvaluation;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;

/**
 * Rewrites the inventory_changed criterion evaluation so it stops doing work it cannot need.
 *
 * <p>The vanilla version copies the predicate array into a list and removes matches one by one, which allocates a
 * lambda and shifts the list on every match; a bitmap of unmatched predicates replaces both. On top of that the
 * predicates are grouped by the item they name, because {@code ItemPredicate.matches} returns false immediately when
 * the stack holds another item - so a slot only has to be tested against the predicates for its own item plus the
 * predicates that accept any item, instead of every predicate registered in the pack.</p>
 *
 * <p>Slot occupancy, empty and full counts are only accumulated when the corresponding bound is actually bounded, and
 * when all three bounds are unbounded the scan stops as soon as the last required predicate has matched, because
 * nothing after that point can change the result.</p>
 */
@Mixin(InventoryChangeTrigger.Instance.class)
public class MixinInventoryChangeTrigger$Instance implements IndexedInventoryCriterion {

    @Shadow
    @Final
    private ItemPredicate[] items;

    @Shadow
    @Final
    private MinMaxBounds full;

    @Shadow
    @Final
    private MinMaxBounds empty;

    @Shadow
    @Final
    private MinMaxBounds occupied;

    @Override
    public boolean nova$needsFullSlotCount() {
        return full != MinMaxBounds.UNBOUNDED;
    }

    @Override
    public boolean nova$testIndexed(final InventoryEvaluation inventory) {
        if (!full.test(inventory.full()) || !empty.test(inventory.empty())
            || !occupied.test(inventory.occupied())) {
            return false;
        }
        for (final ItemPredicate predicate : items) {
            if (!inventory.matches(((AccessorItemPredicate) predicate).nova$getItem(), predicate)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Predicate indices per named item, and the indices of the predicates that accept any item. Built on the first
     * evaluation of this criterion; the predicate array of an instance never changes afterwards.
     */
    @Unique
    private Reference2ObjectMap<Item, int[]> nova$byItem;

    @Unique
    private int[] nova$anyItem;

    /**
     * Scratch bitmap reused across evaluations.
     *
     * <p>Every evaluation needs the same "all predicates still unmatched" bitmap, sized by the predicate count. The
     * criterion is evaluated on the server thread only - {@code Listeners.trigger} walks its listeners in a plain loop
     * - so one buffer per instance is safe, and its length is keyed to the predicate array the instance was built
     * with, which never changes.</p>
     */
    @Unique
    private long[] nova$unmatchedScratch;

    /**
     * @author circulation
     * @reason Test only the predicates that can match the slot's item, through a bitmap of unmatched predicates, and
     * skip counting that no bound consumes.
     */
    @Overwrite
    public boolean test(InventoryPlayer inventory) {
        final ItemPredicate[] predicates = this.items;
        final int predicateCount = predicates.length;
        final boolean countFull = this.full != MinMaxBounds.UNBOUNDED;
        final boolean countEmpty = this.empty != MinMaxBounds.UNBOUNDED;
        final boolean countOccupied = this.occupied != MinMaxBounds.UNBOUNDED;
        final boolean countSlots = countFull || countEmpty || countOccupied;

        Reference2ObjectMap<Item, int[]> byItem = this.nova$byItem;
        if (byItem == null) {
            byItem = this.nova$buildItemBuckets();
        }
        final int[] anyItem = this.nova$anyItem;

        final int wordCount = (predicateCount + Long.SIZE - 1) / Long.SIZE;
        long[] unmatched = this.nova$unmatchedScratch;
        if (unmatched == null || unmatched.length != wordCount) {
            unmatched = new long[wordCount];
            this.nova$unmatchedScratch = unmatched;
        }
        Arrays.fill(unmatched, -1L);

        final int bitsInLastWord = predicateCount & (Long.SIZE - 1);
        if (bitsInLastWord != 0) {
            unmatched[wordCount - 1] = (1L << bitsInLastWord) - 1L;
        }

        int fullCount = 0;
        int emptyCount = 0;
        int occupiedCount = 0;
        int remaining = predicateCount;
        final int inventorySize = inventory.getSizeInventory();

        for (int slot = 0; slot < inventorySize; slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                if (countEmpty) {
                    emptyCount++;
                }
                continue;
            }

            if (countSlots) {
                occupiedCount++;
                if (countFull && stack.getCount() >= stack.getMaxStackSize()) {
                    fullCount++;
                }
            }

            final int[] byNamedItem = byItem.get(stack.getItem());
            if (byNamedItem != null) {
                remaining = nova$match(predicates, unmatched, remaining, byNamedItem, stack);
            }
            if (anyItem.length != 0) {
                remaining = nova$match(predicates, unmatched, remaining, anyItem, stack);
            }

            if (remaining == 0 && !countSlots) {
                break;
            }
        }

        return this.full.test(fullCount)
                && this.empty.test(emptyCount)
                && this.occupied.test(occupiedCount)
                && remaining == 0;
    }

    @Unique
    private int nova$match(final ItemPredicate[] predicates, final long[] unmatched, final int remaining,
                           final int[] indices, final ItemStack stack) {
        int left = remaining;
        for (final int index : indices) {
            final int word = index >>> 6;
            final long bit = 1L << (index & (Long.SIZE - 1));
            if ((unmatched[word] & bit) == 0L) {
                continue;
            }
            if (predicates[index].test(stack)) {
                unmatched[word] &= ~bit;
                if (--left == 0) {
                    return 0;
                }
            }
        }
        return left;
    }

    @Unique
    private Reference2ObjectMap<Item, int[]> nova$buildItemBuckets() {
        final ItemPredicate[] predicates = this.items;
        final Reference2ObjectMap<Item, IntArrayList> byItem = new Reference2ObjectOpenHashMap<>();
        final IntArrayList anyItem = new IntArrayList();
        for (int index = 0; index < predicates.length; index++) {
            final Item item = ((AccessorItemPredicate) predicates[index]).nova$getItem();
            if (item == null) {
                anyItem.add(index);
            } else {
                byItem.computeIfAbsent(item, key -> new IntArrayList()).add(index);
            }
        }

        final Reference2ObjectMap<Item, int[]> frozen = new Reference2ObjectOpenHashMap<>(byItem.size());
        for (final Reference2ObjectMap.Entry<Item, IntArrayList> entry : byItem.reference2ObjectEntrySet()) {
            frozen.put(entry.getKey(), entry.getValue().toIntArray());
        }
        this.nova$anyItem = anyItem.toIntArray();
        this.nova$byItem = frozen;
        return frozen;
    }
}
