package github.kasuminova.novaeng.mixin.enumcache.rtg;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rtg.compat.ModCompat.Mods;

@Mixin(value = Mods.class, remap = false)
public abstract class MixinMods {

    @Shadow
    @Final
    private static Mods[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Mods[] values() {
        return $VALUES;
    }
}
