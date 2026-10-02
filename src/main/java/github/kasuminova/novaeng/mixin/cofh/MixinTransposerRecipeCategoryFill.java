package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeCategoryFill;
import mezz.jei.api.ingredients.IIngredientRegistry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

/**
 * Stops the transposer's JEI fill category from synthesising an entry per fluid container item.
 *
 * <p>After handing out the machine's registered recipes, {@code getRecipes} walks every item stack JEI knows about
 * and, for each one carrying the fluid handler item capability, runs the fill probe and adds a container display
 * for pouring that fluid into it. One row per bucket, tank and cell across the whole pack - generated display
 * entries, not recipes.</p>
 *
 * <p>The ingredient scan is answered with an empty list, so the loop turns idle and none of those wrappers are
 * built or probed; everything the category registers besides them - the machine's own recipes and its catalyst
 * entry - passes through the original code untouched.</p>
 */
@Mixin(value = TransposerRecipeCategoryFill.class, remap = false)
public class MixinTransposerRecipeCategoryFill {

    @Redirect(
        method = "getRecipes",
        at = @At(value = "INVOKE",
            target = "Lmezz/jei/api/ingredients/IIngredientRegistry;getIngredients(Ljava/lang/Class;)Ljava/util/List;"),
        remap = false,
        require = 1
    )
    private static List<ItemStack> nova$skipContainerScan(final IIngredientRegistry registry, final Class<ItemStack> type) {
        if (type != ItemStack.class) {
            return registry.getIngredients(type);
        }
        return Collections.emptyList();
    }
}
