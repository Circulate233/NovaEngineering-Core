package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.plugins.crafttweaker.EnumGunStat;

@Mixin(value = EnumGunStat.class, remap = false)
public abstract class MixinEnumGunStat {

    @Shadow
    @Final
    private static EnumGunStat[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumGunStat[] values() {
        return $VALUES;
    }
}
