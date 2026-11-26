package github.kasuminova.novaeng.mixin.cookingforblockheads;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import github.kasuminova.novaeng.common.performance.StoredRecipeOutput;
import net.blay09.mods.cookingforblockheads.registry.CookingRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CookingRegistry.class, remap = false)
public abstract class MixinCookingRecipeOutput {
    @WrapOperation(method = "initFoodRegistry", at = @At(value = "INVOKE", ordinal = 0, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private static ItemStack nova$rememberFoodOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                                     @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        final ItemStack stack = original.call(recipe);
        prior.set(stack);
        return stack;
    }

    @WrapOperation(method = "initFoodRegistry", at = @At(value = "INVOKE", ordinal = 1, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private static ItemStack nova$reuseStoredFoodOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                                        @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        return StoredRecipeOutput.isDirect(recipe) ? prior.get() : original.call(recipe);
    }
}
