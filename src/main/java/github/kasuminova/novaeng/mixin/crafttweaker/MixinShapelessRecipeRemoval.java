package github.kasuminova.novaeng.mixin.crafttweaker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.mc1120.item.MCItemStack;
import crafttweaker.mc1120.recipes.MCRecipeManager;
import github.kasuminova.novaeng.common.performance.StoredRecipeOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MCRecipeManager.ActionRemoveShapelessRecipes.class, remap = false)
public abstract class MixinShapelessRecipeRemoval {
    @Shadow
    IIngredient output;

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", ordinal = 0, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private ItemStack nova$rememberOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                          @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        final ItemStack stack = original.call(recipe);
        prior.set(stack);
        return stack;
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE", ordinal = 1, remap = true,
        target = "Lnet/minecraft/item/crafting/IRecipe;getRecipeOutput()Lnet/minecraft/item/ItemStack;"), require = 1)
    private ItemStack nova$reuseStoredOutput(final IRecipe recipe, final Operation<ItemStack> original,
                                             @Share("recipeOutput") final LocalRef<ItemStack> prior) {
        return StoredRecipeOutput.isDirect(recipe) ? prior.get() : original.call(recipe);
    }

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
        target = "Lcrafttweaker/api/minecraft/CraftTweakerMC;getIItemStackForMatching(Lnet/minecraft/item/ItemStack;)Lcrafttweaker/api/item/IItemStack;"),
        require = 1)
    private IItemStack nova$skipUnrelatedStackWrapper(final ItemStack stack, final Operation<IItemStack> original) {
        final IIngredient selector = output;
        if (selector != null && selector.getClass() == MCItemStack.class
            && selector.getInternal() instanceof ItemStack target && !target.hasTagCompound()
            && stack != null && target.getItem() != stack.getItem()) {
            // An ordinary item selector rejects null before doing any additional matching.
            return null;
        }
        return original.call(stack);
    }
}
