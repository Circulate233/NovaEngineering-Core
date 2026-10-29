package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.DrumVariant;

@Mixin(value = DrumVariant.class, remap = false)
public abstract class MixinDrumVariant {

    @Shadow
    @Final
    private static DrumVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DrumVariant[] values() {
        return $VALUES;
    }
}
