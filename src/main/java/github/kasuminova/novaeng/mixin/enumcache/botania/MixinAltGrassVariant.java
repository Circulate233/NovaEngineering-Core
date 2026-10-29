package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.AltGrassVariant;

@Mixin(value = AltGrassVariant.class, remap = false)
public abstract class MixinAltGrassVariant {

    @Shadow
    @Final
    private static AltGrassVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AltGrassVariant[] values() {
        return $VALUES;
    }
}
