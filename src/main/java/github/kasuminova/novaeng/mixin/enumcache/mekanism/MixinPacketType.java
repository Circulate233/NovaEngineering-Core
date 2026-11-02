package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketTransmitterUpdate.PacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketType.class, remap = false)
public abstract class MixinPacketType {

    @Shadow
    @Final
    private static PacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketType[] values() {
        return $VALUES;
    }
}
