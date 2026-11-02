package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.mc1120.recipes.MCRecipeManager;
import github.kasuminova.novaeng.common.performance.RecipeRemovalIndex;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(value = MCRecipeManager.ActionRemoveRecipesNoIngredients.class, remap = false)
public abstract class MixinRecipeOutputRemoval {

    @Unique
    private RecipeRemovalIndex nova$removalIndex;
    @Unique
    private boolean nova$indexAttempted;

    @Inject(method = {"apply", "addOutput"}, at = @At("HEAD"), require = 1)
    private void nova$invalidateIndex(final CallbackInfo ci) {
        nova$removalIndex = null;
        nova$indexAttempted = false;
    }

    @Inject(method = "apply", at = @At("RETURN"), require = 1)
    private void nova$releaseIndex(final CallbackInfo ci) {
        nova$removalIndex = null;
    }

    @Redirect(method = "matches", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"), require = 1)
    private Iterator<Pair<IIngredient, Boolean>> nova$skipImpossiblePrefix(
        final List<Pair<IIngredient, Boolean>> outputs, final IItemStack stack) {
        if (!nova$indexAttempted) {
            nova$removalIndex = RecipeRemovalIndex.create(outputs);
            nova$indexAttempted = true;
        }
        if (nova$removalIndex == null) {
            return outputs.iterator();
        }
        // Keep the original scan after the first possible match: a later matcher can
        // invoke item/NBT callbacks and change the input or another selector's state.
        return outputs.listIterator(nova$removalIndex.firstCandidate(stack));
    }
}
