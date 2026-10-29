package github.kasuminova.novaeng.mixin.enumcache.aroma1997core;

import aroma1997.core.client.util.Colors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Colors.class, remap = false)
public abstract class MixinColors {

    @Shadow
    @Final
    private static Colors[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Colors[] values() {
        return $VALUES;
    }
}
