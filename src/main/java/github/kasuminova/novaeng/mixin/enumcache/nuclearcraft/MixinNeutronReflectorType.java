package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.NeutronReflectorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NeutronReflectorType.class, remap = false)
public abstract class MixinNeutronReflectorType {

    @Shadow
    @Final
    private static NeutronReflectorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NeutronReflectorType[] values() {
        return $VALUES;
    }
}
