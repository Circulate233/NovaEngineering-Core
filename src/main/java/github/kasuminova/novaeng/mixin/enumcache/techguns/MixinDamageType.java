package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.api.damagesystem.DamageType;

@Mixin(value = DamageType.class, remap = false)
public abstract class MixinDamageType {

    @Shadow
    @Final
    private static DamageType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DamageType[] values() {
        return $VALUES;
    }
}
