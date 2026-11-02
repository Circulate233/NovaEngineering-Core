package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.filter.gui.DamageMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DamageMode.class, remap = false)
public abstract class MixinDamageMode {

    @Shadow
    @Final
    private static DamageMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DamageMode[] values() {
        return $VALUES;
    }
}
