package github.kasuminova.novaeng.mixin.ic2;

import ic2.core.uu.LeanItemStack;
import ic2.core.uu.RecipeTransformation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = RecipeTransformation.class, remap = false)
public abstract class MixinRecipeTransformation {
    @Shadow
    public List<List<LeanItemStack>> inputs;
    @Shadow
    public List<LeanItemStack> outputs;

    /**
     * @author circulation
     * @reason Remove unused input comparisons and linked-list copies during UU graph construction.
     */
    @Overwrite
    protected void merge() {
        final List<List<LeanItemStack>> mergedInputs = new ObjectArrayList<>(inputs.size());
        LeanItemStack[] matches = null;
        for (final List<LeanItemStack> incoming : inputs) {
            boolean merged = false;
            for (int i = 0; i < mergedInputs.size(); i++) {
                final List<LeanItemStack> existing = mergedInputs.get(i);
                final int size = incoming.size();
                if (size != existing.size()) continue;
                if (size > 1 && (matches == null || matches.length < size)) matches = new LeanItemStack[size];
                final List<LeanItemStack> combined = nova$mergeEqualLists(incoming, existing, matches);
                if (combined != null) {
                    mergedInputs.set(i, combined);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                mergedInputs.add(incoming);
            }
        }
        // Upstream's second pass only creates and consumes temporary LinkedLists. It never
        // changes either the old inputs or mergedInputs, nor uses its match result.
        inputs = mergedInputs;

        final List<LeanItemStack> mergedOutputs = new ObjectArrayList<>(outputs.size());
        for (final LeanItemStack incoming : outputs) {
            boolean merged = false;
            for (int i = 0; i < mergedOutputs.size(); i++) {
                final LeanItemStack existing = mergedOutputs.get(i);
                if (incoming.hasSameItem(existing)) {
                    mergedOutputs.set(i, existing.copyWithSize(existing.getSize() + incoming.getSize()));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                mergedOutputs.add(incoming);
            }
        }
        outputs = mergedOutputs;
    }

    /**
     * @author circulation
     * @reason Match in one array and create combined stacks only after the whole candidate matches.
     */
    @Overwrite
    private List<LeanItemStack> mergeEqualLists(final List<LeanItemStack> incoming,
                                                final List<LeanItemStack> existing) {
        return nova$mergeEqualLists(incoming, existing, null);
    }

    @Unique
    private static List<LeanItemStack> nova$mergeEqualLists(final List<LeanItemStack> incoming,
                                                           final List<LeanItemStack> existing,
                                                           final LeanItemStack[] scratch) {
        final int size = incoming.size();
        if (size != existing.size()) {
            return null;
        }
        if (size == 1) {
            final LeanItemStack first = incoming.get(0);
            final LeanItemStack second = existing.get(0);
            if (!first.hasSameItem(second)) {
                return null;
            }
            final List<LeanItemStack> result = new ObjectArrayList<>(1);
            result.add(first.copyWithSize(first.getSize() + second.getSize()));
            return result;
        }
        if (size == 0) {
            return new ObjectArrayList<>(0);
        }

        // One merge owns the scratch array. Each candidate overwrites the active
        // prefix before matching, and no result or field retains it afterwards.
        final LeanItemStack[] matches = existing.toArray(scratch == null ? new LeanItemStack[size] : scratch);
        int matched = 0;
        for (final LeanItemStack stack : incoming) {
            int index = matched;
            while (index < size && !stack.hasSameItem(matches[index])) {
                index++;
            }
            if (index == size) {
                return null;
            }
            // Keep the first unmatched existing stack, as LinkedList.iterator().remove did.
            // A stable move preserves the order of all remaining alternatives.
            final LeanItemStack selected = matches[index];
            System.arraycopy(matches, matched, matches, matched + 1, index - matched);
            matches[matched++] = selected;
        }

        final List<LeanItemStack> result = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final LeanItemStack stack = incoming.get(i);
            result.add(stack.copyWithSize(stack.getSize() + matches[i].getSize()));
        }
        return result;
    }
}
