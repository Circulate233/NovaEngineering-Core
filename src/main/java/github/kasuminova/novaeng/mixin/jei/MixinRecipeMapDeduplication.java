package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.HeiIndexCapacity;
import github.kasuminova.novaeng.common.performance.HeiRecipeIndexCompaction;
import github.kasuminova.novaeng.common.performance.HeiRecipeInsertion;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.collect.ListMultiMap;
import mezz.jei.collect.Table;
import mezz.jei.recipes.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class MixinRecipeMapDeduplication implements HeiRecipeIndexCompaction {
    @Shadow
    @Final
    private IIngredientRegistry ingredientRegistry;

    @Shadow
    @Final
    private Table<IRecipeCategory, String, List<IRecipeWrapper>> recipeWrapperTable;

    @Shadow @Final
    private ListMultiMap<String, String> categoryUidMap;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "mezz/jei/collect/ListMultiMap"))
    private ListMultiMap<String, String> nova$smallCategoryLists() {
        return new ListMultiMap<>(() -> new ObjectArrayList<>(1));
    }

    @Override
    public void nova$compactRecipeIndexes() {
        HeiIndexCapacity.trimTable(recipeWrapperTable);
        HeiIndexCapacity.trimMultiMap(categoryUidMap);
    }

    /**
     * @author circulation
     * @reason Allocate deduplication storage only after a second distinct expanded ingredient UID.
     */
    @Overwrite
    private <T extends IRecipeWrapper, V> void addRecipe(final T wrapper, final IRecipeCategory<T> category,
                                                         final IIngredientType<V> type, final List<V> ingredients) {
        final var helper = ingredientRegistry.getIngredientHelper(type);
        final var row = recipeWrapperTable.getRow(category);
        HeiRecipeInsertion.insert((RecipeMap) (Object) this, wrapper, category, helper, row, ingredients,
            false);
    }
}
