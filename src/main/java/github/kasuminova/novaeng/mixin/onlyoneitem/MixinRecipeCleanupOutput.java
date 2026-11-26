package github.kasuminova.novaeng.mixin.onlyoneitem;

import com.circulation.only_one_item.handler.MatchItemHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import github.kasuminova.novaeng.common.performance.StoredRecipeOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MatchItemHandler.class, remap = false)
public abstract class MixinRecipeCleanupOutput {
    @WrapOperation(method = "clearRecipe", at = @At(value = "INVOKE", ordinal = 0, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private static ItemStack nova$rememberRecipeOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                                       @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        final ItemStack stack = original.call(recipe);
        prior.set(stack);
        return stack;
    }

    @WrapOperation(method = "clearRecipe", at = @At(value = "INVOKE", ordinal = 1, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private static ItemStack nova$reuseStoredRecipeOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                                          @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        return StoredRecipeOutput.isDirect(recipe) ? prior.get() : original.call(recipe);
    }
}
