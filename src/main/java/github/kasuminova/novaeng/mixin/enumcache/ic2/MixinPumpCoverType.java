package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.logistics.PumpCoverType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PumpCoverType.class, remap = false)
public abstract class MixinPumpCoverType {

    @Shadow
    @Final
    private static PumpCoverType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PumpCoverType[] values() {
        return $VALUES;
    }
}
