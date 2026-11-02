package github.kasuminova.novaeng.mixin.mantle;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import slimeknights.mantle.util.RecipeMatch;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Mixin(value = RecipeMatch.ItemCombination.class, remap = false)
public abstract class MixinRecipeItemCombination extends RecipeMatch {

    @Shadow
    @Final
    protected NonNullList<ItemStack> itemStacks;

    protected MixinRecipeItemCombination() {
        super(0, 0);
    }

    /**
     * @author circulation
     * @reason Avoid match-local boxed index sets for small combinations and unused result lists.
     */
    @Overwrite
    public Optional<Match> matches(final NonNullList<ItemStack> stacks) {
        if (this.itemStacks.size() > 16) {
            return nova$matchLargeCombination(stacks);
        }
        // Integer indices 0..15 occupy distinct, ascending buckets in the original default HashSet.
        int needed = 0;
        for (int index = 0; index < this.itemStacks.size(); index++) {
            if (!this.itemStacks.get(index).isEmpty()) {
                needed |= 1 << index;
            }
        }
        List<ItemStack> found = null;
        for (final ItemStack stack : stacks) {
            for (int candidates = needed; candidates != 0; candidates &= candidates - 1) {
                final int index = Integer.numberOfTrailingZeros(candidates);
                final ItemStack template = this.itemStacks.get(index);
                if (ItemStack.areItemsEqual(template, stack) && ItemStack.areItemStackTagsEqual(template, stack)) {
                    final ItemStack copy = stack.copy();
                    copy.setCount(1);
                    if (found == null) {
                        found = new LinkedList<>();
                    }
                    found.add(copy);
                    needed &= ~(1 << index);
                    break;
                }
            }
        }
        if (needed != 0) {
            return Optional.empty();
        }
        // Match.stacks remains mutable, including a successful combination with no required slots.
        return Optional.of(new Match(found == null ? new LinkedList<>() : found, this.amountMatched));
    }

    @Unique
    private Optional<Match> nova$matchLargeCombination(final NonNullList<ItemStack> stacks) {
        final List<ItemStack> found = new LinkedList<>();
        final IntSet needed = new IntOpenHashSet();
        for (int index = 0; index < this.itemStacks.size(); index++) {
            if (!this.itemStacks.get(index).isEmpty()) {
                needed.add(index);
            }
        }
        for (final ItemStack stack : stacks) {
            final var indices = needed.intIterator();
            while (indices.hasNext()) {
                final ItemStack template = this.itemStacks.get(indices.nextInt());
                if (ItemStack.areItemsEqual(template, stack) && ItemStack.areItemStackTagsEqual(template, stack)) {
                    final ItemStack copy = stack.copy();
                    copy.setCount(1);
                    found.add(copy);
                    indices.remove();
                    break;
                }
            }
        }
        return needed.isEmpty() ? Optional.of(new Match(found, this.amountMatched)) : Optional.empty();
    }
}
