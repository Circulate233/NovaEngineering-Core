package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.LivingRockVariant;

@Mixin(value = LivingRockVariant.class, remap = false)
public abstract class MixinLivingRockVariant {

    @Shadow
    @Final
    private static LivingRockVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LivingRockVariant[] values() {
        return $VALUES;
    }
}
