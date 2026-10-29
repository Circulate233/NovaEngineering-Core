package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.NeutronSourceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NeutronSourceType.class, remap = false)
public abstract class MixinNeutronSourceType {

    @Shadow
    @Final
    private static NeutronSourceType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NeutronSourceType[] values() {
        return $VALUES;
    }
}
