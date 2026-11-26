package github.kasuminova.novaeng.common.performance;

import mezz.jei.api.recipe.IRecipeCategory;

public interface HeiRecipeCategoryIndex {
    void nova$addRecipeCategoryWithUid(IRecipeCategory<?> category, String ingredientUid, boolean shareStrings);
}
