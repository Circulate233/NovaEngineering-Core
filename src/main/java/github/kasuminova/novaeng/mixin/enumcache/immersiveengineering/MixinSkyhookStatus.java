package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.api.CapabilitySkyhookData.SkyhookStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SkyhookStatus.class, remap = false)
public abstract class MixinSkyhookStatus {

    @Shadow
    @Final
    private static SkyhookStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SkyhookStatus[] values() {
        return $VALUES;
    }
}
