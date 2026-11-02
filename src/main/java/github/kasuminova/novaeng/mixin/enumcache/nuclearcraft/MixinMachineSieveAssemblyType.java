package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.MachineSieveAssemblyType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineSieveAssemblyType.class, remap = false)
public abstract class MixinMachineSieveAssemblyType {

    @Shadow
    @Final
    private static MachineSieveAssemblyType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MachineSieveAssemblyType[] values() {
        return $VALUES;
    }
}
