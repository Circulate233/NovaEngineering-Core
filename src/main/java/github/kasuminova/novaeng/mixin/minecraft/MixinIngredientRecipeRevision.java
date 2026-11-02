package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.IngredientInputs;
import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ingredient.class)
public abstract class MixinIngredientRecipeRevision implements IngredientInputs {
    @Shadow
    @Final
    public ItemStack[] matchingStacks;

    @Inject(method = "invalidateAll", at = @At("HEAD"), require = 1, remap = false)
    private static void nova$observeIngredientInvalidation(final CallbackInfo ci) {
        StartupCraftingScope.ingredientsChanged();
    }

    @Override
    public ItemStack[] nova$rawIngredientInputs() {
        return matchingStacks;
    }
}
