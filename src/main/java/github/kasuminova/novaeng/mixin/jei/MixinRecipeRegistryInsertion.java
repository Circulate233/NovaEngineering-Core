package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.IngredientGroups;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.ingredients.Ingredients;
import mezz.jei.recipes.RecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;

@Mixin(value = RecipeRegistry.class, remap = false)
public abstract class MixinRecipeRegistryInsertion {
    @Redirect(method = "addRecipeUnchecked", at = @At(value = "INVOKE",
        target = "Lmezz/jei/ingredients/Ingredients;getInputIngredients()Ljava/util/Map;"), require = 1)
    private Map<?, ?> nova$flattenInputs(final Ingredients ingredients) {
        return nova$flattenGroups(((AccessorIngredients) ingredients).nova$getInputs());
    }

    @Redirect(method = "addRecipeUnchecked", at = @At(value = "INVOKE",
        target = "Lmezz/jei/ingredients/Ingredients;getOutputIngredients()Ljava/util/Map;"), require = 1)
    private Map<?, ?> nova$flattenOutputs(final Ingredients ingredients) {
        return nova$flattenGroups(((AccessorIngredients) ingredients).nova$getOutputs());
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<IIngredientType<?>, List<?>> nova$flattenGroups(
        final Map<IIngredientType<?>, List<List<?>>> groups) {
        // Retain HEI's map layout and callback order; only eliminate list growth and per-group toArray copies.
        final Map<IIngredientType<?>, List<?>> flattened = new Reference2ObjectOpenHashMap<>();
        for (final var entry : groups.entrySet()) {
            flattened.put(entry.getKey(), IngredientGroups.flatten((List) entry.getValue()));
        }
        return flattened;
    }
}
