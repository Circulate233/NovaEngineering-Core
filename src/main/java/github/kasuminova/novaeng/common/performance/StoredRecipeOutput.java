package github.kasuminova.novaeng.common.performance;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * These implementations return the same stack field every time, including when it is mutated in place.
 */
public final class StoredRecipeOutput {
    private StoredRecipeOutput() {
    }

    public static boolean isDirect(final IRecipe recipe) {
        final Class<?> type = recipe.getClass();
        return type == ShapedRecipes.class || type == ShapelessRecipes.class
            || type == ShapedOreRecipe.class || type == ShapelessOreRecipe.class;
    }
}
