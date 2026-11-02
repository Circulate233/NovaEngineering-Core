package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.filter.gui.DamageModeIconHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DamageModeIconHolder.class, remap = false)
public abstract class MixinDamageModeIconHolder {

    @Shadow
    @Final
    private static DamageModeIconHolder[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DamageModeIconHolder[] values() {
        return $VALUES;
    }
}
