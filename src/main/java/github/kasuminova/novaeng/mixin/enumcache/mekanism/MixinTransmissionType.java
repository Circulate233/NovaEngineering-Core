package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.api.transmitters.TransmissionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TransmissionType.class, remap = false)
public abstract class MixinTransmissionType {

    @Shadow
    @Final
    private static TransmissionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TransmissionType[] values() {
        return $VALUES;
    }
}
