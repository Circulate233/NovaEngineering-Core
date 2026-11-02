package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.Internal;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.ingredients.IngredientRegistry;
import mezz.jei.ingredients.Ingredients;
import mezz.jei.recipes.RecipeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

@Mixin(value = RecipeRegistry.class, remap = false)
public abstract class MixinRecipeRegistryId {

    @Shadow
    @Final
    private IngredientRegistry ingredientRegistry;

    /**
     * @author circulation
     * @reason Hash the existing ingredient groups without allocating flattened maps, lists, or copies.
     */
    @Overwrite
    private long calculateId(final IRecipeCategory<?> category, final Ingredients ingredients) {
        long hash = 0L;
        final Set<IIngredientType<?>> craftableTypes = Internal.getIngredientRegistry().getCraftableIngredientTypes();
        for (final IIngredientType<?> type : craftableTypes) {
            hash = this.nova$hashIngredientGroups(hash, ingredients.getInputs(type));
        }
        for (final IIngredientType<?> type : craftableTypes) {
            hash = this.nova$hashIngredientGroups(hash, ingredients.getOutputs(type));
        }
        return category.getUid().hashCode() + hash * 31L;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Unique
    private long nova$hashIngredientGroups(long hash, final List<? extends List<?>> groups) {
        Class<?> previousClass = null;
        IIngredientHelper helper = null;
        // Retain the helper across adjacent groups as well as adjacent values of the same class.
        for (int groupIndex = 0, groupCount = groups.size(); groupIndex < groupCount; groupIndex++) {
            final List<?> values = groups.get(groupIndex);
            if (values instanceof RandomAccess) {
                for (int index = 0, size = values.size(); index < size; index++) {
                    final Object ingredient = values.get(index);
                    if (ingredient == null) {
                        continue;
                    }
                    final Class<?> ingredientClass = ingredient.getClass();
                    if (ingredientClass != previousClass) {
                        helper = this.ingredientRegistry.getIngredientHelper(ingredient);
                        previousClass = ingredientClass;
                    }
                    hash = helper.getHash(ingredient) + hash * 31L;
                }
                continue;
            }
            for (final Object ingredient : values) {
                if (ingredient == null) {
                    continue;
                }
                final Class<?> ingredientClass = ingredient.getClass();
                if (ingredientClass != previousClass) {
                    helper = this.ingredientRegistry.getIngredientHelper(ingredient);
                    previousClass = ingredientClass;
                }
                hash = helper.getHash(ingredient) + hash * 31L;
            }
        }
        return hash;
    }
}
