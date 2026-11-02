package github.kasuminova.novaeng.mixin.enderio;

import crazypants.enderio.base.recipe.lookup.ItemRecipeLeafNode;
import crazypants.enderio.base.recipe.lookup.ItemRecipeNode;
import crazypants.enderio.base.recipe.lookup.TriItemLookup;
import github.kasuminova.novaeng.common.performance.TriItemLookupRootAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TriItemLookup.class, remap = false)
public abstract class MixinTriItemLookupAccess<R> implements TriItemLookupRootAccess<R> {
    @Shadow
    @Final
    private ItemRecipeNode<R, ItemRecipeNode<R, ItemRecipeLeafNode<R>>> root;

    @Override
    public ItemRecipeNode<R, ItemRecipeNode<R, ItemRecipeLeafNode<R>>> nova$getRecipeRoot() {
        return root;
    }
}
