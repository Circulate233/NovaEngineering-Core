package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketPortableTeleporter.PortableTeleporterPacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PortableTeleporterPacketType.class, remap = false)
public abstract class MixinPortableTeleporterPacketType {

    @Shadow
    @Final
    private static PortableTeleporterPacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PortableTeleporterPacketType[] values() {
        return $VALUES;
    }
}
