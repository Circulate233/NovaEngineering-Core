package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.concurrent.RecipeCraftingContextPool;
import github.kasuminova.novaeng.common.performance.RecipeContextPool;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;

@Mixin(value = RecipeCraftingContextPool.class, remap = false)
public abstract class MixinRecipeCraftingContextPool {
    /**
     * @author NovaEngineering
     * @reason Bound idle retention without locking initialization work.
     */
    @Nonnull
    @Overwrite
    public static RecipeCraftingContext borrowCtx(final ActiveMachineRecipe recipe,
                                                  final TileMultiblockMachineController controller) {
        return RecipeContextPool.borrow(recipe, controller);
    }

    /**
     * @author NovaEngineering
     * @reason Reject returns across reload and release unused requirement copies.
     */
    @Overwrite
    public static void returnCtx(final RecipeCraftingContext context) {
        RecipeContextPool.release(context);
    }

    /**
     * @author NovaEngineering
     * @reason Invalidate borrowed and idle contexts as one generation.
     */
    @Overwrite
    public static void onReload() {
        RecipeContextPool.reset();
    }
}
