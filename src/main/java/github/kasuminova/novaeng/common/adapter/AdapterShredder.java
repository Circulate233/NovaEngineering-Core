package github.kasuminova.novaeng.common.adapter;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.novaeng.common.adapter.util.HashedItemStack;
import github.kasuminova.novaeng.common.adapter.util.IC2MachineRecipeConverter;
import github.kasuminova.novaeng.common.adapter.util.MekCrusherRecipeConverter;
import github.kasuminova.novaeng.common.adapter.util.NCOBasicRecipeConverter;
import github.kasuminova.novaeng.common.adapter.util.PulverizerRecipeConverter;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdapterShredder extends RecipeAdapter {

    protected int sortId = 0;

    public AdapterShredder() {
        super(new ResourceLocation("novaeng_core", "shredder"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(final ResourceLocation owningMachineName, final List<RecipeModifier> modifiers, final List<ComponentRequirement<?, ?>> additionalRequirements, final Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers, final List<String> recipeTooltips) {
        Set<HashedItemStack> registeredStack = new ObjectOpenHashSet<>();
        List<MachineRecipe> recipes = new ObjectArrayList<>();

        recipes.addAll(PulverizerRecipeConverter.convert(
                stack -> createRecipeShell(owningMachineName, HashedItemStack.stackToString(stack), modifiers), stacks -> checkStackRegistered(registeredStack, stacks), modifiers));
        recipes.addAll(NCOBasicRecipeConverter.convert(
                stack -> createRecipeShell(owningMachineName, HashedItemStack.stackToString(stack), modifiers), stacks -> checkStackRegistered(registeredStack, stacks), modifiers, 10));
        recipes.addAll(MekCrusherRecipeConverter.convert(
                stack -> createRecipeShell(owningMachineName, HashedItemStack.stackToString(stack), modifiers), stacks -> checkStackRegistered(registeredStack, stacks), modifiers, 10));
        recipes.addAll(IC2MachineRecipeConverter.convertMaceratorRecipes(
                stack -> createRecipeShell(owningMachineName, HashedItemStack.stackToString(stack), modifiers), stacks -> checkStackRegistered(registeredStack, stacks), modifiers, 10));

        incId++;
        return recipes;
    }

    protected MachineRecipe createRecipeShell(final ResourceLocation owningMachineName, final String suffix, final List<RecipeModifier> modifiers) {
        MachineRecipe machineRecipe = createRecipeShell(new ResourceLocation("novaeng_core", "shredder_" + suffix + "_" + incId),
                owningMachineName, Math.round(RecipeModifier.applyModifiers(
                        modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, 200, false)),
                sortId, false
        );
        sortId++;
        return machineRecipe;
    }

    protected boolean checkStackRegistered(Set<HashedItemStack> registeredStack, List<ItemStack> stacks) {
        List<HashedItemStack> checked = new ObjectArrayList<>();
        for (final ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                return false;
            }
            HashedItemStack hashedStack = HashedItemStack.ofTagUnsafe(stack);
            if (registeredStack.contains(hashedStack)) {
                return false;
            }
            checked.add(hashedStack.copy());
        }
        registeredStack.addAll(checked);
        return true;
    }

    @Override
    public void resetIncId() {
        super.resetIncId();
        sortId = 0;
    }
}
