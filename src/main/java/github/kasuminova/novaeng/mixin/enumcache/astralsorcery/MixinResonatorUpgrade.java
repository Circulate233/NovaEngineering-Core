package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.item.tool.ItemSkyResonator.ResonatorUpgrade;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ResonatorUpgrade.class, remap = false)
public abstract class MixinResonatorUpgrade {

    @Shadow
    @Final
    private static ResonatorUpgrade[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ResonatorUpgrade[] values() {
        return $VALUES;
    }
}
