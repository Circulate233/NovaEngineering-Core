package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.recipes.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class MixinRecipeMapDeduplication {

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
