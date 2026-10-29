package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.QuartzVariant;

@Mixin(value = QuartzVariant.class, remap = false)
public abstract class MixinQuartzVariant {

    @Shadow
    @Final
    private static QuartzVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static QuartzVariant[] values() {
        return $VALUES;
    }
}
