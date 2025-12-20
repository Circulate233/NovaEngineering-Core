package github.kasuminova.novaeng.mixin.ic2;

import com.llamalad7.mixinextras.sugar.Local;
import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.BasicMachineRecipeManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(value = BasicMachineRecipeManager.class,remap = false)
public abstract class MixinBasicMachineRecipeManager {

    @Shadow
    protected abstract void displayError(String msg);

    @Redirect(method = "addRecipe(Lic2/api/recipe/IRecipeInput;Ljava/util/Collection;Lnet/minecraft/nbt/NBTTagCompound;Z)Z", at = @At(value = "INVOKE", target = "Lic2/core/recipe/BasicMachineRecipeManager;displayError(Ljava/lang/String;)V"))
    private void new$displayError(BasicMachineRecipeManager basicMachineRecipeManager, String string, @Local(name = "input") IRecipeInput input, @Local(name = "output") Collection<ItemStack> output) {
        this.displayError("Input ItemStack " + input.getInputs().stream().map(ItemStack::getDisplayName).collect(Collectors.toList()) + " ,output ItemStack " + output.stream().map(ItemStack::getDisplayName).collect(Collectors.toList()) + " is invalid.");
    }
}