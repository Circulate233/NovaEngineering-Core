package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.stackable.Things;
import crazypants.enderio.base.config.recipes.ShapedRecipe;
import crazypants.enderio.util.Prep;
import github.kasuminova.novaeng.common.performance.SingleRecipeOutput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShapedRecipe.class, remap = false)
public abstract class MixinShapedRecipeOutput extends ShapedRecipes {
    @Shadow
    @Final
    protected Things result;

    private MixinShapedRecipeOutput() {
        super("", 0, 0, null, ItemStack.EMPTY);
    }

    @Inject(method = "getRecipeOutput", at = @At("HEAD"), remap = true, cancellable = true, require = 1)
    private void nova$readSingleResult(final CallbackInfoReturnable<ItemStack> cir) {
        if (result instanceof SingleRecipeOutput access) {
            final ItemStack stack = access.nova$singleRecipeOutput();
            if (stack != null) {
                cir.setReturnValue(Prep.isValid(stack) ? stack : super.getRecipeOutput());
            }
        }
    }
}
