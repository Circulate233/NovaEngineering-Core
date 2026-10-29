package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketSecurityUpdate.SecurityPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SecurityPacket.class, remap = false)
public abstract class MixinSecurityPacket {

    @Shadow
    @Final
    private static SecurityPacket[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SecurityPacket[] values() {
        return $VALUES;
    }
}
