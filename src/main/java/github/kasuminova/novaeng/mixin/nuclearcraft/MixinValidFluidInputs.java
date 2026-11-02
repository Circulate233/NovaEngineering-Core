package github.kasuminova.novaeng.mixin.nuclearcraft;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import github.kasuminova.novaeng.common.performance.ValidFluidInputs;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import nc.recipe.BasicRecipeHandler;
import nc.recipe.RecipeHelper;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = RecipeHelper.class, remap = false)
public abstract class MixinValidFluidInputs {

    @Redirect(method = "validFluids(Lnc/recipe/BasicRecipeHandler;Ljava/util/Set;)Ljava/util/List;",
        at = @At(value = "NEW", target = "it/unimi/dsi/fastutil/objects/ObjectOpenHashSet"), require = 1)
    private static ObjectOpenHashSet<String> nova$indexInputs(final BasicRecipeHandler recipeHandler, final Set<String> exceptions,
                                                              @Share("nova$fluidInputs") final LocalRef<ValidFluidInputs> index) {
        index.set(ValidFluidInputs.create(recipeHandler, exceptions));
        return new ObjectOpenHashSet<>();
    }

    @Redirect(method = "validFluids(Lnc/recipe/BasicRecipeHandler;Ljava/util/Set;)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Lnc/recipe/BasicRecipeHandler;isValidFluidInput(Lnet/minecraftforge/fluids/FluidStack;)Z"), require = 1)
    private static boolean nova$testInputs(final BasicRecipeHandler handler, final FluidStack stack,
                                           @Share("nova$fluidInputs") final LocalRef<ValidFluidInputs> index) {
        final ValidFluidInputs inputs = index.get();
        return inputs == null ? handler.isValidFluidInput(stack) : inputs.test(stack);
    }
}
