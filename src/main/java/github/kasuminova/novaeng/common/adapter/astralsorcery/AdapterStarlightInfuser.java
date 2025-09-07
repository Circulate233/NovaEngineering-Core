package github.kasuminova.novaeng.common.adapter.astralsorcery;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.novaeng.common.machine.IllumPool;
import github.kasuminova.novaeng.common.util.Function;
import hellfirepvp.astralsorcery.common.crafting.ItemHandle;
import hellfirepvp.astralsorcery.common.crafting.infusion.AbstractInfusionRecipe;
import hellfirepvp.astralsorcery.common.crafting.infusion.InfusionRecipeRegistry;
import hellfirepvp.astralsorcery.common.crafting.infusion.recipes.InfusionRecipeChargeTool;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterStarlightInfuser extends RecipeAdapter {

    public AdapterStarlightInfuser() {
        super(new ResourceLocation("astralsorcery", "starlight_infuser"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(final ResourceLocation owningMachineName,
                                                      final List<RecipeModifier> modifiers,
                                                      final List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      final Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      final List<String> recipeTooltips) {
        int ids = InfusionRecipeRegistry.recipes.size() + InfusionRecipeRegistry.mtRecipes.size();

        List<MachineRecipe> recipes = new ArrayList<>();
        for (int id = 0; id < ids; id++) {
            AbstractInfusionRecipe infusionRecipe = InfusionRecipeRegistry.getRecipe(id);
            if (infusionRecipe == null) {
                continue;
            }
            if (infusionRecipe instanceof InfusionRecipeChargeTool) {
                continue;
            }

            MachineRecipe recipe = createRecipeShell(
                    new ResourceLocation("astralsorcery", "infusion_" + incId),
                    owningMachineName,
                    20, 0, false);

            // Item / Fluid Input
            ItemHandle input = infusionRecipe.getInput();
            switch (input.handleType) {
                case STACK -> {
                    for (final ItemStack applicableItem : input.getApplicableItems()) {
                        int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, applicableItem.getCount(), false));
                        if (inAmount > 0) {
                            recipe.addRequirement(new RequirementItem(IOType.INPUT, ItemUtils.copyStackWithSize(applicableItem, inAmount)));
                        }
                    }
                }
                case OREDICT -> {
                    String oreDictName = input.getOreDictName();
                    int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, 1, false));
                    if (inAmount > 0) {
                        recipe.addRequirement(new RequirementItem(IOType.INPUT, oreDictName, inAmount));
                    }
                }
                case FLUID -> {
                    FluidStack fluidStack = input.getFluidTypeAndAmount();
                    assert fluidStack != null;
                    int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.INPUT, fluidStack.amount, false));
                    if (inAmount > 0) {
                        FluidStack copied = fluidStack.copy();
                        copied.amount = inAmount;
                        recipe.addRequirement(new RequirementFluid(IOType.INPUT, copied));
                    }
                }
            }
            // Item Output
            ItemStack output = infusionRecipe.getOutput(null);
            int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, output.getCount(), false));
            if (outAmount > 0) {
                recipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(output, outAmount)));
            }

            // Mana input
            float consumeChance = infusionRecipe.getLiquidStarlightConsumptionChance();
            int craftingTickTime = infusionRecipe.craftingTickTime();
            int manaToConsume = Math.round(craftingTickTime * 10 * (1F - consumeChance));
            recipe.addRecipeEventHandler(RecipeCheckEvent.class, (IEventHandler<RecipeCheckEvent>) event -> {
                if (event.phase != Phase.START) return;
                IllumPool.onRecipeCheck(event, manaToConsume);
            });
            recipe.addRecipeEventHandler(FactoryRecipeFinishEvent.class, IllumPool::onRecipeFinished);
            recipe.addRecipeEventHandler(FactoryRecipeTickEvent.class, (IEventHandler<FactoryRecipeTickEvent>) event -> {
                if (event.phase != Phase.START) return;
                IllumPool.onRecipeTick(event, manaToConsume);
            });
            recipe.addTooltip(Function.getText("novaeng.illum_pool.input.mana", manaToConsume));
            addStarlightCatalystHandler(recipe);
            recipe.addTooltip(Function.getText("novaeng.illum_pool.illum_pool.mode",
                    Function.getText("top.illum_pool.mode.d")));
            recipe.addTooltip("novaeng.illum_pool.input.illum");

            recipes.add(recipe);
            incId++;
        }

        return recipes;
    }

    protected static void addStarlightCatalystHandler(final MachineRecipe recipe) {
        recipe.addRecipeEventHandler(RecipeCheckEvent.class, (IEventHandler<RecipeCheckEvent>) event -> {
            if (event.phase != Phase.START) return;
            TileMultiblockMachineController controller = event.getController();
            boolean hasCatalyst = false;
            if (controller.hasModifierReplacement(IllumPool.ALCHEMY_CATALYST)) {
                hasCatalyst = true;
            } else if (controller.hasModifierReplacement(IllumPool.CONJURATION_CATALYST)) {
                hasCatalyst = true;
            } else if (controller.hasModifierReplacement(IllumPool.DIMENSION_CATALYST)) {
                hasCatalyst = true;
            }
            if (hasCatalyst) {
                event.setFailed("novaeng.illum_pool.failed.input");
                return;
            }
            if (!controller.hasModifierReplacement(IllumPool.STARLIGHT_CATALYST)) {
                event.setFailed("novaeng.illum_pool.failed.input");
            }
        });
    }
}
