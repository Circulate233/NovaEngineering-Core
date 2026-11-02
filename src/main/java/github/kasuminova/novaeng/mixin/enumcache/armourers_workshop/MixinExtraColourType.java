package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.api.common.IExtraColours.ExtraColourType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ExtraColourType.class, remap = false)
public abstract class MixinExtraColourType {

    @Shadow
    @Final
    private static ExtraColourType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ExtraColourType[] values() {
        return $VALUES;
    }
}
