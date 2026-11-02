package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.client.effect.EntityComplexFX.AlphaFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AlphaFunction.class, remap = false)
public abstract class MixinAlphaFunction {

    @Shadow
    @Final
    private static AlphaFunction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AlphaFunction[] values() {
        return $VALUES;
    }
}
