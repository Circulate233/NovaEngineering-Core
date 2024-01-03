package github.kasuminova.novaeng.common.adapter.nco;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.nco.AdapterNCOMachine;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import nc.recipe.BasicRecipe;
import nc.recipe.NCRecipes;
import nc.recipe.ingredient.IFluidIngredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterNCOElectrolyzer extends AdapterNCOMachine {
    public static final int WORK_TIME = 1600;
    public static final int BASE_ENERGY_USAGE = 40;

    public AdapterNCOElectrolyzer() {
        super(new ResourceLocation("nuclearcraft", "electrolyzer"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(final ResourceLocation owningMachineName, final List<RecipeModifier> modifiers, final List<ComponentRequirement<?, ?>> additionalRequirements, final Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers, final List<String> recipeTooltips) {
        List<BasicRecipe> recipeList = NCRecipes.electrolyzer.getRecipeList();
        List<MachineRecipe> machineRecipeList = new ArrayList<>(recipeList.size());

        for (BasicRecipe basicRecipe : recipeList) {
            MachineRecipe recipe = createRecipeShell(new ResourceLocation("nuclearcraft", "electrolyzer_" + incId),
                    owningMachineName, (int) basicRecipe.getBaseProcessTime(Math.round(RecipeModifier.applyModifiers(
                            modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, WORK_TIME, false))),
                    incId, false
            );

            for (IFluidIngredient fluidIngredient : basicRecipe.getFluidIngredients()) {
                FluidStack copied = fluidIngredient.getStack().copy();
                int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.INPUT, copied.amount, false));
                if (inAmount > 0) {
                    copied.amount = inAmount;
                    recipe.addRequirement(new RequirementFluid(IOType.INPUT, copied));
                }
            }

            for (IFluidIngredient fluidProduct : basicRecipe.getFluidProducts()) {
                FluidStack productStack = fluidProduct.getStack();
                if (productStack == null) {
                    continue;
                }
                FluidStack copied = productStack.copy();
                int outputAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.OUTPUT, copied.amount, false));
                if (outputAmount > 0) {
                    copied.amount = outputAmount;
                    recipe.addRequirement(new RequirementFluid(IOType.OUTPUT, copied));
                }
            }

            recipe.addRequirement(new RequirementEnergy(IOType.INPUT, Math.round(RecipeModifier.applyModifiers(
                    modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, BASE_ENERGY_USAGE, false)))
            );

            machineRecipeList.add(recipe);
            incId++;
        }

        return machineRecipeList;
    }
}
