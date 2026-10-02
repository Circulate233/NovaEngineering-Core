package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeCategoryExtract;
import mezz.jei.api.ingredients.IIngredientRegistry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

/**
 * Stops the transposer's JEI extract category from synthesising an entry per fluid container item.
 *
 * <p>Besides the machine's registered recipes, {@code getRecipes} walks every item stack JEI knows about and, for
 * each one carrying the fluid handler item capability, runs the drain probe and adds a container display for
 * draining that item into its fluid. That probe is also what makes this category the expensive half of the
 * transposer's JEI setup, since it is a fluid drain attempt per container item in the pack.</p>
 *
 * <p>The ingredient scan is answered with an empty list, so the loop turns idle and neither the probes nor the
 * generated rows happen; the machine's own recipes (potion draining among them) and the catalyst entry come
 * through the original code untouched.</p>
 */
@Mixin(value = TransposerRecipeCategoryExtract.class, remap = false)
public class MixinTransposerRecipeCategoryExtract {

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
