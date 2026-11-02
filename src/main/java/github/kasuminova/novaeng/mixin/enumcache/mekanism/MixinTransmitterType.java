package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.block.states.BlockStateTransmitter.TransmitterType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TransmitterType.class, remap = false)
public abstract class MixinTransmitterType {

    @Shadow
    @Final
    private static TransmitterType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TransmitterType[] values() {
        return $VALUES;
    }
}
