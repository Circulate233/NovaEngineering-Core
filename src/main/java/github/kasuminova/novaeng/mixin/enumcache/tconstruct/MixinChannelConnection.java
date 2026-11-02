package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.smeltery.tileentity.TileChannel.ChannelConnection;

@Mixin(value = ChannelConnection.class, remap = false)
public abstract class MixinChannelConnection {

    @Shadow
    @Final
    private static ChannelConnection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChannelConnection[] values() {
        return $VALUES;
    }
}
