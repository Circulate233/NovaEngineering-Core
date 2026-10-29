package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketRadiationData.RadiationPacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RadiationPacketType.class, remap = false)
public abstract class MixinRadiationPacketType {

    @Shadow
    @Final
    private static RadiationPacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RadiationPacketType[] values() {
        return $VALUES;
    }
}
