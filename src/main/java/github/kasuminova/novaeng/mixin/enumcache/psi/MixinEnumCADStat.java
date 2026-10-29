package github.kasuminova.novaeng.mixin.enumcache.psi;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.psi.api.cad.EnumCADStat;

@Mixin(value = EnumCADStat.class, remap = false)
public abstract class MixinEnumCADStat {

    @Shadow
    @Final
    private static EnumCADStat[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumCADStat[] values() {
        return $VALUES;
    }
}
