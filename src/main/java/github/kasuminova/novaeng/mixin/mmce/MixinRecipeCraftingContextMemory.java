package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.RecipeContextAccess;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

@Mixin(value = RecipeCraftingContext.class, remap = false)
public abstract class MixinRecipeCraftingContextMemory implements RecipeContextAccess {
    @Shadow
    @Final
    private List<ComponentRequirement<?, ?>> requirements;

    @Shadow
    @Final
    private Map<Long, Collection<ProcessingComponent<?>>> typeComponents;
    @Shadow
    @Final
    private Map<Long, List<RequirementComponents>> requirementComponents;
    @Shadow
    @Final
    private long[][] groupKeys;
    @Shadow
    private long groupId;

    @Shadow
    @Final
    private List<ComponentOutputRestrictor> currentRestrictions;

    @Shadow
    public abstract RecipeCraftingContext setGroupId(long i);

    @Shadow
    private static List<ProcessingComponent<?>> getCopiedRequirementComponents(
        final RecipeCraftingContext.ReqCompMap plain, final RecipeCraftingContext.TaggedReqCompMap tagged,
        final ComponentRequirement<?, ?> requirement, final List<ProcessingComponent<?>> components) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Build the retained component groups directly, avoiding a temporary map and lambdas per requirement.
     */
    @Overwrite
    public void updateRequirementComponents() {
        requirementComponents.clear();
        final RecipeCraftingContext context = (RecipeCraftingContext) (Object) this;
        // Every candidate group must contain every requirement, including its missing-component check.
        if (typeComponents.isEmpty()) {
            requirementComponents.put(0L, new ObjectArrayList<>(requirements.size()));
        } else {
            for (final Long key : typeComponents.keySet()) {
                requirementComponents.put(key, new ObjectArrayList<>(requirements.size()));
            }
        }
        for (final ComponentRequirement<?, ?> requirement : requirements) {
            for (final var group : requirementComponents.entrySet()) {
                ObjectArrayList<ProcessingComponent<?>> valid = null;
                final Collection<ProcessingComponent<?>> candidates = typeComponents.get(group.getKey());
                if (candidates != null) {
                    if (candidates instanceof List<?> candidateList && candidateList instanceof RandomAccess) {
                        for (int i = 0, size = candidateList.size(); i < size; i++) {
                            final ProcessingComponent<?> component =
                                (ProcessingComponent<?>) candidateList.get(i);
                            if (requirement.isValidComponent(component, context)
                                && (requirement.getTag() == null || requirement.getTag().equals(component.getTag()))) {
                                if (valid == null) valid = new ObjectArrayList<>(2);
                                valid.add(component);
                            }
                        }
                    } else {
                        for (final ProcessingComponent<?> component : candidates) {
                            if (requirement.isValidComponent(component, context)
                                && (requirement.getTag() == null || requirement.getTag().equals(component.getTag()))) {
                                if (valid == null) valid = new ObjectArrayList<>(2);
                                valid.add(component);
                            }
                        }
                    }
                }
                group.getValue().add(new RequirementComponents(requirement,
                    valid == null ? ObjectLists.emptyList() : valid));
            }
        }
        for (final long key : requirementComponents.keySet()) {
            if (key >= 0) {
                setGroupId(key);
                break;
            }
        }
        final long[] keys = new long[requirementComponents.size()];
        int index = 0;
        for (final long key : requirementComponents.keySet()) keys[index++] = key;
        Arrays.sort(keys);
        groupKeys[0] = keys;
        if (groupKeys[1] == null) groupKeys[1] = new long[1];
        groupKeys[1][0] = groupId;
    }

    /**
     * @author NovaEngineering
     * @reason Allocate deferred single-component errors only after the first failure; keep commit-on-failure order.
     */
    @Overwrite
    private boolean canStartCrafting(final RecipeCraftingContext.CraftingCheckResult result,
                                     final RequirementComponents reqComponent,
                                     final RecipeCraftingContext.ReqCompMap plain,
                                     final RecipeCraftingContext.TaggedReqCompMap tagged) {
        final RecipeCraftingContext context = (RecipeCraftingContext) (Object) this;
        final ComponentRequirement<?, ?> requirement = reqComponent.requirement();
        requirement.startRequirementCheck(ResultChance.GUARANTEED, context);
        final List<ProcessingComponent<?>> components = reqComponent.components();
        if (!components.isEmpty()) {
            if (requirement instanceof ComponentRequirement.MultiComponent multi) {
                final CraftCheck check = multi.canStartCrafting(
                    getCopiedRequirementComponents(plain, tagged, requirement, components), context);
                if (check.isSuccess()) return true;
                result.addError(check.getUnlocalizedMessage());
                return false;
            }
            ObjectArrayList<String> errors = null;
            if (components instanceof RandomAccess) {
                for (int i = 0, size = components.size(); i < size; i++) {
                    final ProcessingComponent<?> component = components.get(i);
                    final CraftCheck check = requirement.canStartCrafting(component, context, currentRestrictions);
                    if (check.isSuccess()) {
                        requirement.endRequirementCheck();
                        return true;
                    }
                    if (!check.isInvalid() && !check.getUnlocalizedMessage().isEmpty()) {
                        if (errors == null) errors = new ObjectArrayList<>(2);
                        errors.add(check.getUnlocalizedMessage());
                    }
                }
            } else {
                for (final ProcessingComponent<?> component : components) {
                    final CraftCheck check = requirement.canStartCrafting(component, context, currentRestrictions);
                    if (check.isSuccess()) {
                        requirement.endRequirementCheck();
                        return true;
                    }
                    if (!check.isInvalid() && !check.getUnlocalizedMessage().isEmpty()) {
                        if (errors == null) errors = new ObjectArrayList<>(2);
                        errors.add(check.getUnlocalizedMessage());
                    }
                }
            }
            if (errors != null) {
                for (int i = 0; i < errors.size(); i++) result.addError(errors.get(i));
            }
        } else {
            result.addError(requirement.getMissingComponentErrorMessage(requirement.getActionType()));
        }
        requirement.endRequirementCheck();
        return false;
    }

    @Override
    public void nova$discardIdleRequirements() {
        requirements.clear();
    }
}
