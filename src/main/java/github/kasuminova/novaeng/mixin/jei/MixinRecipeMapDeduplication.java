package github.kasuminova.novaeng.mixin.jei;

import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.collect.ListMultiMap;
import mezz.jei.recipes.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class MixinRecipeMapDeduplication {

    @Shadow @Final
    private ListMultiMap<String, String> categoryUidMap;

    @Redirect(method = "addRecipe(Lmezz/jei/api/recipe/IRecipeWrapper;Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Lmezz/jei/recipes/RecipeMap;addRecipeCategory(Lmezz/jei/api/recipe/IRecipeCategory;Ljava/lang/Object;Lmezz/jei/api/ingredients/IIngredientHelper;)V"), require = 1)
    private <T> void nova$reuseIngredientId(final RecipeMap map, final IRecipeCategory<?> category,
                                          final T ingredient, final IIngredientHelper<T> helper,
                                          @Local final String key) {
        if (map.getClass() != RecipeMap.class) {
            map.addRecipeCategory(category, ingredient, helper);
            return;
        }
        final List<String> categories = categoryUidMap.get(key);
        final String categoryId = category.getUid();
        if (!categories.contains(categoryId)) {
            categories.add(categoryId);
        }
    }

    @Redirect(method = "addRecipe(Lmezz/jei/api/recipe/IRecipeWrapper;Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 1)
    private boolean nova$checkAndRememberId(final Set<String> uniqueIds, final Object key) {
        return !uniqueIds.add((String) key);
    }

    @Redirect(method = "addRecipe(Lmezz/jei/api/recipe/IRecipeWrapper;Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"), require = 1)
    private boolean nova$idAlreadyRemembered(final Set<String> uniqueIds, final Object key) {
        // The local set was updated by the immediately preceding membership check.
        return true;
    }
}
