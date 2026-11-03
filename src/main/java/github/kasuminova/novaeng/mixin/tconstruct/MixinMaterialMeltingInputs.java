package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.MeltingRecipeIndex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;
import slimeknights.tconstruct.plugin.jei.material.MaterialWrapper;

import java.util.List;

@Mixin(value = MaterialWrapper.class, remap = false)
public abstract class MixinMaterialMeltingInputs {
    @Shadow
    @Final
    Material material;

    @Redirect(method = "getInputs", at = @At(value = "INVOKE",
        target = "Lslimeknights/tconstruct/library/TinkerRegistry;getAllMeltingRecipies()Ljava/util/List;"), require = 1)
    private List<MeltingRecipe> nova$matchingFluidCandidates() {
        return MeltingRecipeIndex.candidates(material.getFluid());
    }
}
