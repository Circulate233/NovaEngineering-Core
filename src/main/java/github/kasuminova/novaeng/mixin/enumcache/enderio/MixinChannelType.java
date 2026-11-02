package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.transceiver.ChannelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChannelType.class, remap = false)
public abstract class MixinChannelType {

    @Shadow
    @Final
    private static ChannelType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChannelType[] values() {
        return $VALUES;
    }
}
