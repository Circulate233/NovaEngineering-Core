package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.CoolantHeaterType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CoolantHeaterType.class, remap = false)
public abstract class MixinCoolantHeaterType {

    @Shadow
    @Final
    private static CoolantHeaterType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CoolantHeaterType[] values() {
        return $VALUES;
    }
}
