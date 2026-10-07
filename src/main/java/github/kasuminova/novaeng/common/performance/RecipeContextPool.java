package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;

/**
 * MMCE adapter; no context initialization, reset or destruction occurs while the pool is locked.
 */
public final class RecipeContextPool {
    private static final BoundedContextPool<ResourceLocation, RecipeCraftingContext> IDLE = new BoundedContextPool<>();

    private RecipeContextPool() {
    }

    public static RecipeCraftingContext borrow(final ActiveMachineRecipe recipe,
                                               final TileMultiblockMachineController controller) {
        final int generation = IDLE.generation();
        final RecipeCraftingContext context = IDLE.take(recipe.getRecipe().getRegistryName(), generation);
        return context == null
            ? new RecipeCraftingContext(generation, recipe, controller)
            : context.init(recipe, controller);
    }

    public static void release(final RecipeCraftingContext context) {
        final int generation = context.getReloadCounter();
        final ResourceLocation recipe = context.getParentRecipe().getRegistryName();
        context.resetAll();
        ((RecipeContextAccess) context).nova$discardIdleRequirements();
        // resetAll clears the active recipe/components; destroy() would call resetAll a second time.
        IDLE.offer(recipe, context, generation, NovaEngCoreConfig.PERFORMANCE.recipeContextsPerRecipe,
            NovaEngCoreConfig.PERFORMANCE.recipeContextLimit);
    }

    public static void reset() {
        IDLE.reset();
    }

    public static int idleCount() {
        return IDLE.size();
    }
}
