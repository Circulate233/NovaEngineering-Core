package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import github.kasuminova.novaeng.mixin.jei.AccessorItemStackHelper;
import github.kasuminova.novaeng.mixin.jei.AccessorStackHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackHelper;
import mezz.jei.recipes.RecipeMap;
import mezz.jei.startup.StackHelper;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Insertion order and virtual category callbacks match installed HEI 4.36.1.
 */
public final class HeiRecipeInsertion {
    private HeiRecipeInsertion() {
    }

    public static <T extends IRecipeWrapper, V> void insert(
        final RecipeMap map, final T wrapper, final IRecipeCategory<T> category,
        final IIngredientHelper<V> helper, final Map<String, List<IRecipeWrapper>> row,
        final List<V> ingredients) {
        insert(map, wrapper, category, helper, row, ingredients, true);
    }

    public static <T extends IRecipeWrapper, V> void insert(
        final RecipeMap map, final T wrapper, final IRecipeCategory<T> category,
        final IIngredientHelper<V> helper, final Map<String, List<IRecipeWrapper>> row,
        final List<V> ingredients, final boolean shareStrings) {
        final AccessorStackHelper cache = activeItemUidCache(map, helper);
        boolean seenFirst = false;
        String firstId = null;
        Set<String> ids = null;
        // Expansion may mutate the input or return a different list. Keep its iterator semantics.
        for (final V ingredient : helper.expandSubtypes(ingredients)) {
            if (ingredient == null) {
                continue;
            }
            final String rawId = helper.getUniqueId(ingredient);
            final String id = shareStrings ? StringCanon.value(rawId) : rawId;
            if (!seenFirst) {
                seenFirst = true;
                firstId = id;
            } else if (ids == null) {
                if (Objects.equals(id, firstId)) {
                    continue;
                }
                ids = new ObjectOpenHashSet<>();
                ids.add(firstId);
                ids.add(id);
            } else if (!ids.add(id)) {
                continue;
            }
            final List<IRecipeWrapper> wrappers = row.get(id);
            if (wrappers == null) {
                // RecipeMap exposes immutable query results; keep a singleton until another
                // wrapper needs the same ingredient/category entry.
                row.put(id, ObjectLists.singleton(wrapper));
            } else if (wrappers instanceof ObjectLists.Singleton<?>) {
                final ObjectArrayList<IRecipeWrapper> expanded = new ObjectArrayList<>(2);
                expanded.add(wrappers.get(0));
                expanded.add(wrapper);
                row.put(id, expanded);
            } else {
                wrappers.add(wrapper);
            }
            // HEI would immediately read the same identity-cached UID for this stack again.
            if (cache != null && cache.nova$isUidCacheEnabled() && !((ItemStack) ingredient).isEmpty()) {
                ((HeiRecipeCategoryIndex) map).nova$addRecipeCategoryWithUid(category, id, shareStrings);
            } else {
                map.addRecipeCategory(category, ingredient, helper);
            }
        }
    }

    private static AccessorStackHelper activeItemUidCache(final RecipeMap map, final IIngredientHelper<?> helper) {
        if (map.getClass() != RecipeMap.class || !(map instanceof HeiRecipeCategoryIndex)
            || helper.getClass() != ItemStackHelper.class || !(helper instanceof AccessorItemStackHelper accessor))
            return null;
        final StackHelper stacks = accessor.nova$getStackHelper();
        return stacks.getClass() == StackHelper.class && stacks instanceof AccessorStackHelper state ? state : null;
    }
}
