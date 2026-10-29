package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.PylonVariant;

@Mixin(value = PylonVariant.class, remap = false)
public abstract class MixinPylonVariant {

    @Shadow
    @Final
    private static PylonVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PylonVariant[] values() {
        return $VALUES;
    }
}
