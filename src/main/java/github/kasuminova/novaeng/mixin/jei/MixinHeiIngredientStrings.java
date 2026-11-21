package github.kasuminova.novaeng.mixin.jei;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.kasuminova.novaeng.common.performance.string.StringCanon;
import mezz.jei.recipes.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = RecipeMap.class, remap = false, priority = 1100)
public abstract class MixinHeiIngredientStrings {
    // This mixin is selected only when sharing is enabled. The insertion path
    // receives a constant instead of reading configuration for every recipe.
    // The exact descriptor also selects the private method merged by the index mixin.
    @ModifyArg(method = "addRecipe(Lmezz/jei/api/recipe/IRecipeWrapper;Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V", at = @At(value = "INVOKE",
        target = "Lgithub/kasuminova/novaeng/common/performance/HeiRecipeInsertion;insert(Lmezz/jei/recipes/RecipeMap;Lmezz/jei/api/recipe/IRecipeWrapper;Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/ingredients/IIngredientHelper;Ljava/util/Map;Ljava/util/List;Z)V"),
        index = 6, require = 0)
    private boolean nova$shareInsertedIds(final boolean ignored) {
        return true;
    }

    @ModifyExpressionValue(method = "addRecipeCategory", at = @At(value = "INVOKE",
        target = "Lmezz/jei/api/ingredients/IIngredientHelper;getUniqueId(Ljava/lang/Object;)Ljava/lang/String;"))
    private String nova$ingredientId(final String value) {
        return StringCanon.value(value);
    }

    @ModifyExpressionValue(method = "addRecipeCategory", at = @At(value = "INVOKE",
        target = "Lmezz/jei/api/recipe/IRecipeCategory;getUid()Ljava/lang/String;"))
    private String nova$categoryId(final String value) {
        return StringCanon.value(value);
    }
}
