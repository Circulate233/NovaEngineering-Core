package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.mixin.util.NovaRAB;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeAdapterBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = MachineRecipe.class, remap = false)
public abstract class MixinMachineRecipe {

    @Shadow
    @Final
    protected List<ComponentRequirement<?, ?>> recipeRequirements;

    @Inject(method = "mergeAdapter", at = @At("HEAD"))
    public void mergeAdapter(RecipeAdapterBuilder adapterBuilder, CallbackInfo ci) {
        if (adapterBuilder instanceof NovaRAB n) {
            String inTagName = n.n$getInTags();
            String outTagName = n.n$getOutTags();
            if (inTagName.isEmpty() && outTagName.isEmpty()) return;
            ComponentSelectorTag inTag = inTagName.isEmpty() ? null : new ComponentSelectorTag(n.n$getInTags());
            ComponentSelectorTag outTag = outTagName.isEmpty() ? null : new ComponentSelectorTag(n.n$getOutTags());
            this.recipeRequirements.forEach(component -> {
                if (component instanceof RequirementItem) {
                    switch (component.getActionType()) {
                        case INPUT -> {
                            if (!inTagName.isEmpty()) {
                                component.setTag(inTag);
                            }
                        }
                        case OUTPUT -> {
                            if (!outTagName.isEmpty()) {
                                component.setTag(outTag);
                            }
                        }
                    }
                }
            });
        }
    }
}
