package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeCategoryFill;
import mezz.jei.api.IModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Removes the transposer's JEI fill (bucket filling) recipe registration.
 *
 * <p>Whether this replaces the method at all is decided by the {@code OptimizeThermalTransposerRecipes} switch
 * before the mixin is applied, so the body is the permanent replacement rather than a conditional skip.</p>
 */
@Mixin(value = TransposerRecipeCategoryFill.class, remap = false)
public class MixinTransposerRecipeCategoryFill {

    /**
     * @author circulation
     * @reason The category derives one entry per container and fluid pair; when this mixin is applied the setup
     *         is meant to be skipped entirely. Mixin application is already gated by the same switch.
     */
    @Overwrite(remap = false)
    public static void initialize(final IModRegistry registry) {
        // Registration is intentionally dropped.
    }

}
