package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.logic.sensor.AreaType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AreaType.class, remap = false)
public abstract class MixinAreaType {

    @Shadow
    @Final
    private static AreaType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AreaType[] values() {
        return $VALUES;
    }
}
