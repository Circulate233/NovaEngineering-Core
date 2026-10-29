package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.Resource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Resource.class, remap = false)
public abstract class MixinResource {

    @Shadow
    @Final
    private static Resource[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Resource[] values() {
        return $VALUES;
    }
}
