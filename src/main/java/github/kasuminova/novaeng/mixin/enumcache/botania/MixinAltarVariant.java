package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.AltarVariant;

@Mixin(value = AltarVariant.class, remap = false)
public abstract class MixinAltarVariant {

    @Shadow
    @Final
    private static AltarVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AltarVariant[] values() {
        return $VALUES;
    }
}
