package github.kasuminova.novaeng.mixin.mets;

import net.lrsoft.mets.enchantment.EfficientEnergyCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EfficientEnergyCost.class, remap = false)
public abstract class MixinEfficientEnergyCost {

    @Shadow
    private static float[] Ratio;

    /**
     * @author circulation
     * @reason Preserve the fallback ratio without allocating an exception for out-of-range levels.
     */
    @Overwrite
    public static float getAttenuationRatio(final int level) {
        final int index = level - 1;
        return Ratio != null && index >= 0 && index < Ratio.length ? Ratio[index] : 1.0F;
    }
}
