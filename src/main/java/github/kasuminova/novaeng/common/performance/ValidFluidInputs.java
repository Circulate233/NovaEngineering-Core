package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import nc.recipe.AbstractRecipeHandler;
import nc.recipe.BasicRecipe;
import nc.recipe.BasicRecipeHandler;
import nc.recipe.ingredient.EmptyFluidIngredient;
import nc.recipe.ingredient.FluidArrayIngredient;
import nc.recipe.ingredient.FluidIngredient;
import nc.recipe.ingredient.IFluidIngredient;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A single validFluids call's standard, untagged, NEUTRAL input membership.
 */
public final class ValidFluidInputs {

    private static final Set<Class<?>> STANDARD_LISTS = Set.of(ArrayList.class,
        Arrays.asList(Void.class, Object.class).getClass(), Collections.emptyList().getClass(), Collections.singletonList(null).getClass());

    private static final ClassValue<Boolean> STANDARD_HANDLER = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                return type.getMethod("isValidFluidInput", FluidStack.class).getDeclaringClass() == AbstractRecipeHandler.class
                    && type.getMethod("getRecipeList").getDeclaringClass() == BasicRecipeHandler.class;
            } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                return false;
            }
        }
    };

    private static final ClassValue<Boolean> IDENTITY_FLUID = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                return type.getMethod("hashCode").getDeclaringClass() == Object.class
                    && type.getMethod("equals", Object.class).getDeclaringClass() == Object.class;
            } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                return false;
            }
        }
    };

    private final BasicRecipeHandler handler;
    private final ReferenceOpenHashSet<Fluid> fluids = new ReferenceOpenHashSet<>();
    private boolean usable = true;

    private ValidFluidInputs(final BasicRecipeHandler handler) {
        this.handler = handler;
    }

    public static ValidFluidInputs create(final BasicRecipeHandler handler, final Set<String> exceptions) {
        // A custom exceptions.contains callback could mutate recipes between fluids.
        // Only the normal empty-exception scan has no such intervening callback.
        if (exceptions != Collections.<String>emptySet() || !STANDARD_HANDLER.get(handler.getClass())) {
            return null;
        }
        final List<BasicRecipe> recipes = handler.getRecipeList();
        if (recipes == null || recipes.getClass() != ArrayList.class || recipes.size() < 16) {
            return null;
        }
        final ValidFluidInputs index = new ValidFluidInputs(handler);
        for (final BasicRecipe recipe : recipes) {
            if (recipe == null || recipe.getClass() != BasicRecipe.class
                || !index.collect(recipe.getFluidIngredients(), 0)) {
                return null;
            }
        }
        return index;
    }

    private boolean collect(final List<IFluidIngredient> ingredients, final int depth) {
        if (ingredients == null || !STANDARD_LISTS.contains(ingredients.getClass()) || depth > 32) {
            return false;
        }
        for (final IFluidIngredient ingredient : ingredients) {
            if (ingredient == null) {
                return false;
            }
            if (ingredient.getClass() == FluidIngredient.class) {
                final FluidStack stack = ((FluidIngredient) ingredient).stack;
                if (stack != null) {
                    if (stack.getClass() != FluidStack.class) {
                        return false;
                    }
                    // validFluids constructs untagged stacks. NEUTRAL ignores amount,
                    // and a tagged recipe stack cannot match an untagged input.
                    if (stack.tag == null) {
                        try {
                            fluids.add(stack.getFluid());
                        } catch (NullPointerException ignored) {
                            // A stack made from an unregistered duplicate Fluid can
                            // lack a delegate. Let the original scan decide whether
                            // that ingredient is reached before another one matches.
                            return false;
                        }
                    }
                }
            } else if (ingredient.getClass() == FluidArrayIngredient.class) {
                if (!collect(((FluidArrayIngredient) ingredient).ingredientList, depth + 1)) {
                    return false;
                }
            } else if (ingredient.getClass() != EmptyFluidIngredient.class) {
                return false;
            }
        }
        return true;
    }

    public boolean test(final FluidStack stack) {
        if (usable && stack != null && stack.getClass() == FluidStack.class && stack.tag == null
            && IDENTITY_FLUID.get(stack.getFluid().getClass())) {
            return fluids.contains(stack.getFluid());
        }
        // FluidStack construction consults a HashMap keyed by Fluid. A custom
        // hash/equals or a custom stack matcher may mutate recipes; stop using this
        // call's index before testing it, including all later inputs in the call.
        usable = false;
        return handler.isValidFluidInput(stack);
    }
}
