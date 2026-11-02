package github.kasuminova.novaeng.common.performance;

import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import crazypants.enderio.base.recipe.lookup.ItemRecipeNode;

public interface TriItemLookupRootAccess<R> {
    ItemRecipeNode<R, ItemRecipeNode<R, ItemRecipeLeafNode<R>>> nova$getRecipeRoot();
}
