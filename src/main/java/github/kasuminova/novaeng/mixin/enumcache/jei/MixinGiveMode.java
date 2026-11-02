package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.util.GiveMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GiveMode.class, remap = false)
public abstract class MixinGiveMode {

    @Shadow
    @Final
    private static GiveMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GiveMode[] values() {
        return $VALUES;
    }
}
