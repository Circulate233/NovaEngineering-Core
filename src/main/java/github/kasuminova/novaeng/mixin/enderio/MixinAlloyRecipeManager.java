package github.kasuminova.novaeng.mixin.enderio;

import crazypants.enderio.base.recipe.IManyToOneRecipe;
import crazypants.enderio.base.recipe.alloysmelter.AlloyRecipeManager;
import crazypants.enderio.base.recipe.lookup.TriItemLookup;
import github.kasuminova.novaeng.common.performance.AlloyRecipeIndexWriter;
import github.kasuminova.novaeng.common.performance.RecipeItemAlternatives;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(value = AlloyRecipeManager.class, remap = false)
public abstract class MixinAlloyRecipeManager {

    /**
     * @author circulation
     * @reason Index item identities once instead of repeating the same metadata alternatives and permutations.
     */
    @Overwrite
    private static void addRecipeToLookup(final TriItemLookup<IManyToOneRecipe> lookup, final IManyToOneRecipe recipe) {
        final var alternatives = recipe.getInputStackAlternatives();
        if (alternatives.isEmpty() || alternatives.size() > 3) {
            return;
        }
        final List<Item> first = RecipeItemAlternatives.uniqueItems(alternatives.getFirst());
        if (first.isEmpty()) {
            return;
        }
        if (alternatives.size() == 1) {
            for (int firstIndex = 0; firstIndex < first.size(); firstIndex++) {
                lookup.addRecipe(recipe, first.get(firstIndex));
            }
            return;
        }
        final List<Item> second = RecipeItemAlternatives.uniqueItems(alternatives.get(1));
        if (second.isEmpty()) {
            return;
        }
        if (alternatives.size() == 2) {
            final AlloyRecipeIndexWriter<IManyToOneRecipe> writer =
                new AlloyRecipeIndexWriter<>(lookup, recipe, first.size() + second.size());
            for (int firstIndex = 0; firstIndex < first.size(); firstIndex++) {
                final Item a = first.get(firstIndex);
                for (int secondIndex = 0; secondIndex < second.size(); secondIndex++) {
                    final Item b = second.get(secondIndex);
                    writer.add(a, b);
                    if (a != b) {
                        writer.add(b, a);
                    }
                }
            }
            return;
        }
        final List<Item> third = RecipeItemAlternatives.uniqueItems(alternatives.get(2));
        final AlloyRecipeIndexWriter<IManyToOneRecipe> writer =
            new AlloyRecipeIndexWriter<>(lookup, recipe, first.size() + second.size() + third.size());
        for (int firstIndex = 0; firstIndex < first.size(); firstIndex++) {
            final Item a = first.get(firstIndex);
            for (int secondIndex = 0; secondIndex < second.size(); secondIndex++) {
                final Item b = second.get(secondIndex);
                for (int thirdIndex = 0; thirdIndex < third.size(); thirdIndex++) {
                    final Item c = third.get(thirdIndex);
                    writer.add(a, b, c);
                    if (b != c) {
                        writer.add(a, c, b);
                    }
                    if (a != b) {
                        writer.add(b, a, c);
                    }
                    if (a != b && a != c) {
                        writer.add(b, c, a);
                    }
                    if (c != a && c != b) {
                        writer.add(c, a, b);
                    }
                    if (a != b && a != c && b != c) {
                        writer.add(c, b, a);
                    }
                }
            }
        }
    }
}
