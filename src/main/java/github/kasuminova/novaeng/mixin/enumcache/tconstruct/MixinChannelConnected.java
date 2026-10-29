package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.gadgets.block.BlockSlimeChannel.ChannelConnected;

@Mixin(value = ChannelConnected.class, remap = false)
public abstract class MixinChannelConnected {

    @Shadow
    @Final
    private static ChannelConnected[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChannelConnected[] values() {
        return $VALUES;
    }
}
