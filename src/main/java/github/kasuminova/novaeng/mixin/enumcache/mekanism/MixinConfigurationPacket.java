package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketConfigurationUpdate.ConfigurationPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigurationPacket.class, remap = false)
public abstract class MixinConfigurationPacket {

    @Shadow
    @Final
    private static ConfigurationPacket[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigurationPacket[] values() {
        return $VALUES;
    }
}
