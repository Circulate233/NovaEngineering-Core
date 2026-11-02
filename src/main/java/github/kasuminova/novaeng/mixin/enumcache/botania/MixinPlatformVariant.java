package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.PlatformVariant;

@Mixin(value = PlatformVariant.class, remap = false)
public abstract class MixinPlatformVariant {

    @Shadow
    @Final
    private static PlatformVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PlatformVariant[] values() {
        return $VALUES;
    }
}
