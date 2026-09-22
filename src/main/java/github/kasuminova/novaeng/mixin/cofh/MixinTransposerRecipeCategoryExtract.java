package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeCategoryExtract;
import mezz.jei.api.IModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Removes the transposer's JEI extract (draining) recipe registration.
 *
 * <p>{@code getRecipes} probes every registered fluid against every item stack by running the drain logic, so
 * this category dominates the transposer's JEI setup cost. Whether this replaces the method at all is decided
 * by the {@code OptimizeThermalTransposerRecipes} switch before the mixin is applied.</p>
 */
@Mixin(value = TransposerRecipeCategoryExtract.class, remap = false)
public class MixinTransposerRecipeCategoryExtract {

    /**
     * @author circulation
     * @reason The category's setup probes every fluid against every stack; when this mixin is applied the
     *         registration is meant to be dropped entirely. Mixin application is already gated by the switch.
     */
    @Overwrite(remap = false)
    public static void initialize(final IModRegistry registry) {
        // Registration is intentionally dropped.
    }

}
