package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.RecipeContextAccess;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
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
    public abstract RecipeCraftingContext setGroupId(long i);

    /**
     * @author NovaEngineering
     * @reason Build the retained component groups directly, avoiding a temporary map and lambdas per requirement.
     */
    @Overwrite
    public void updateRequirementComponents() {
        requirementComponents.clear();
        final RecipeCraftingContext context = (RecipeCraftingContext) (Object) this;
        for (final ComponentRequirement<?, ?> requirement : requirements) {
            boolean matched = false;
            for (final var group : typeComponents.entrySet()) {
                ObjectArrayList<ProcessingComponent<?>> valid = null;
                for (final ProcessingComponent<?> component : group.getValue()) {
                    if (requirement.isValidComponent(component, context)
                        && (requirement.getTag() == null || requirement.getTag().equals(component.getTag()))) {
                        if (valid == null) valid = new ObjectArrayList<>(2);
                        valid.add(component);
                    }
                }
                if (valid != null) {
                    requirementComponents.computeIfAbsent(group.getKey(), key -> new ObjectArrayList<>(2))
                                         .add(new RequirementComponents(requirement, valid));
                    matched = true;
                }
            }
            // Missing requirements still participate in group zero so their error checks are preserved.
            if (!matched) {
                requirementComponents.computeIfAbsent(0L, key -> new ObjectArrayList<>(2))
                                     .add(new RequirementComponents(requirement, ObjectLists.emptyList()));
            }
        }
        if (requirementComponents.isEmpty()) {
            requirementComponents.put(0L, ObjectLists.emptyList());
            setGroupId(0L);
        } else {
            for (final long key : requirementComponents.keySet()) {
                if (key >= 0) {
                    setGroupId(key);
                    break;
                }
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

    @Override
    public void nova$discardIdleRequirements() {
        requirements.clear();
    }
}
