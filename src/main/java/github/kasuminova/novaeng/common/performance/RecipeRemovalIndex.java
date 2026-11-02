package github.kasuminova.novaeng.common.performance;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.mc1120.item.MCItemStack;
import crafttweaker.mc1120.item.MCMutableItemStack;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A conservative starting position for CraftTweaker's original ordered removal scan.
 */
public final class RecipeRemovalIndex {

    private final Reference2IntOpenHashMap<Item> firstByItem;
    private final ItemStack[] selectors;
    private final Item[] indexedItems;

    private RecipeRemovalIndex(final Reference2IntOpenHashMap<Item> firstByItem, final ItemStack[] selectors,
                               final Item[] indexedItems) {
        this.firstByItem = firstByItem;
        this.selectors = selectors;
        this.indexedItems = indexedItems;
    }

    public static RecipeRemovalIndex create(final List<Pair<IIngredient, Boolean>> outputs) {
        if (outputs.getClass() != ArrayList.class || outputs.size() < 16) {
            return null;
        }
        final Reference2IntOpenHashMap<Item> firstByItem = new Reference2IntOpenHashMap<>();
        firstByItem.defaultReturnValue(outputs.size());
        final ItemStack[] selectors = new ItemStack[outputs.size()];
        final Item[] indexedItems = new Item[outputs.size()];
        for (int i = 0; i < outputs.size(); i++) {
            final Pair<IIngredient, Boolean> entry = outputs.get(i);
            if (entry == null || entry.getClass() != ImmutablePair.class || entry.getValue() == null) {
                return null;
            }
            final IIngredient ingredient = entry.getKey();
            if (!isStandardStack(ingredient)) {
                return null;
            }
            final ItemStack stack = (ItemStack) ingredient.getInternal();
            if (stack.isEmpty()) {
                return null;
            }
            final Item item = stack.getItem();
            selectors[i] = stack;
            indexedItems[i] = item;
            firstByItem.putIfAbsent(item, i);
        }
        return new RecipeRemovalIndex(firstByItem, selectors, indexedItems);
    }

    private static boolean isStandardStack(final IIngredient ingredient) {
        return ingredient != null
            && (ingredient.getClass() == MCItemStack.class || ingredient.getClass() == MCMutableItemStack.class);
    }

    public int firstCandidate(final IItemStack input) {
        if (!isStandardStack(input)) {
            return 0;
        }
        final ItemStack stack = (ItemStack) input.getInternal();
        if (stack.isEmpty()) {
            return 0;
        }
        // OnlyOneItem can replace or restore an existing stack's item, including
        // after metadata changes. Check raw selector references before skipping any
        // predicates. Retain old positions as conservative false positives: every
        // currently possible item must start at or before its earliest selector.
        for (int i = 0; i < selectors.length; i++) {
            final Item item = selectors[i].getItem();
            if (item != indexedItems[i]) {
                indexedItems[i] = item;
                if (i < firstByItem.getInt(item)) {
                    firstByItem.put(item, i);
                }
            }
        }
        return firstByItem.getInt(stack.getItem());
    }
}
