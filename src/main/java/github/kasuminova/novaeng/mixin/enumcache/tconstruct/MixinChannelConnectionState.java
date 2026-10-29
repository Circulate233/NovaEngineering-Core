package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.smeltery.block.BlockChannel.ChannelConnectionState;

@Mixin(value = ChannelConnectionState.class, remap = false)
public abstract class MixinChannelConnectionState {

    @Shadow
    @Final
    private static ChannelConnectionState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChannelConnectionState[] values() {
        return $VALUES;
    }
}
