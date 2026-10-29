package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.init.ModObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModObject.class, remap = false)
public abstract class MixinModObject {

    @Shadow
    @Final
    private static ModObject[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModObject[] values() {
        return $VALUES;
    }
}
