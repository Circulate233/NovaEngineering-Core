package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.items.armors.EnumArmorStat;

@Mixin(value = EnumArmorStat.class, remap = false)
public abstract class MixinEnumArmorStat {

    @Shadow
    @Final
    private static EnumArmorStat[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumArmorStat[] values() {
        return $VALUES;
    }
}
