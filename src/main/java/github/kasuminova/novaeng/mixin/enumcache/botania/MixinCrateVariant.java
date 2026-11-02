package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.CrateVariant;

@Mixin(value = CrateVariant.class, remap = false)
public abstract class MixinCrateVariant {

    @Shadow
    @Final
    private static CrateVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CrateVariant[] values() {
        return $VALUES;
    }
}
