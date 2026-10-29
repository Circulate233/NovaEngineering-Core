package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.RadShieldingType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RadShieldingType.class, remap = false)
public abstract class MixinRadShieldingType {

    @Shadow
    @Final
    private static RadShieldingType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RadShieldingType[] values() {
        return $VALUES;
    }
}
