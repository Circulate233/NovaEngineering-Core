package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.RecipeContextAccess;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = RecipeCraftingContext.class, remap = false)
public abstract class MixinRecipeCraftingContextMemory implements RecipeContextAccess {
    @Shadow
    @Final
    private List<ComponentRequirement<?, ?>> requirements;

    @Override
    public void nova$discardIdleRequirements() {
        requirements.clear();
    }
}
