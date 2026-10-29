package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.block.network.BlockAltar.AltarType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AltarType.class, remap = false)
public abstract class MixinAltarType {

    @Shadow
    @Final
    private static AltarType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AltarType[] values() {
        return $VALUES;
    }
}
