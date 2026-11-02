package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.CoolantHeaterType2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CoolantHeaterType2.class, remap = false)
public abstract class MixinCoolantHeaterType2 {

    @Shadow
    @Final
    private static CoolantHeaterType2[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CoolantHeaterType2[] values() {
        return $VALUES;
    }
}
