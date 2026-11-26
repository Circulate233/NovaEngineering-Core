package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(value = RecipeCraftingContext.class, remap = false)
public abstract class MixinRecipeModifierAppliers {
    @Shadow @Final private Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> modifierAppliers;
    @Shadow @Final private Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> chanceModifierAppliers;

    /**
     * @author NovaEngineering
     * @reason Allocate only channels actually used; preserve COW list snapshots and publication order.
     */
    @Overwrite
    public void addModifierApplier(final RequirementType<?, ?> type, final List<RecipeModifier> modifiers) {
        RecipeModifier.ModifierApplier value = null;
        RecipeModifier.ModifierApplier chance = null;
        for (final RecipeModifier modifier : modifiers) {
            if (modifier.affectsChance()) {
                if (chance == null) chance = new RecipeModifier.ModifierApplier();
                RecipeModifier.applyValueToApplier(chance, modifier);
            } else {
                if (value == null) value = new RecipeModifier.ModifierApplier();
                RecipeModifier.applyValueToApplier(value, modifier);
            }
        }
        // Original method intentionally leaves previous entries intact for default results.
        if (value != null && !value.isDefault()) modifierAppliers.put(type, value);
        if (chance != null && !chance.isDefault()) chanceModifierAppliers.put(type, chance);
    }
}
