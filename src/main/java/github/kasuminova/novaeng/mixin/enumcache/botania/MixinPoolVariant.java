package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.PoolVariant;

@Mixin(value = PoolVariant.class, remap = false)
public abstract class MixinPoolVariant {

    @Shadow
    @Final
    private static PoolVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PoolVariant[] values() {
        return $VALUES;
    }
}
