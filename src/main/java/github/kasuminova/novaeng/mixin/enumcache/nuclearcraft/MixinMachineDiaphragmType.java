package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.MachineDiaphragmType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineDiaphragmType.class, remap = false)
public abstract class MixinMachineDiaphragmType {

    @Shadow
    @Final
    private static MachineDiaphragmType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MachineDiaphragmType[] values() {
        return $VALUES;
    }
}
