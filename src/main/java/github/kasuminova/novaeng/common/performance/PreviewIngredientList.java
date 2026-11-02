package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Temporary first-item index for BlockArray's existing, ordered ingredient merge loop.
 */
public final class PreviewIngredientList extends LinkedList<List<ItemStack>> {

    private Reference2ObjectOpenHashMap<Item, List<List<ItemStack>>> byItem = new Reference2ObjectOpenHashMap<>();
    private boolean hasEmptyFirstStack;

    @Override
    public boolean add(final List<ItemStack> ingredients) {
        if (this.byItem != null && !ingredients.isEmpty()) {
            final ItemStack first = ingredients.getFirst();
            if (first.isEmpty()) {
                // Growing an empty stack can change getItem(); leave those unusual lists unindexed.
                this.hasEmptyFirstStack = true;
            } else {
                final Item item = first.getItem();
                List<List<ItemStack>> candidates = this.byItem.computeIfAbsent(item, k -> new ArrayList<>());
                // Include multi-alternative groups: the original loop also merges into their first stack.
                candidates.add(ingredients);
            }
        }
        return super.add(ingredients);
    }

    public Iterator<List<ItemStack>> candidates(final ItemStack input) {
        if (this.byItem == null || this.hasEmptyFirstStack || input.isEmpty()) {
            return iterator();
        }
        final List<List<ItemStack>> candidates = this.byItem.get(input.getItem());
        return candidates == null ? Collections.emptyIterator() : candidates.iterator();
    }

    public void finishBuilding() {
        // Callers receive a normal mutable list; the index is only valid during construction.
        this.byItem = null;
    }
}
