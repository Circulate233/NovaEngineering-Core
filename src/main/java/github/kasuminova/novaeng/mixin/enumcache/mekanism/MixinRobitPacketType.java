package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketRobit.RobitPacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RobitPacketType.class, remap = false)
public abstract class MixinRobitPacketType {

    @Shadow
    @Final
    private static RobitPacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RobitPacketType[] values() {
        return $VALUES;
    }
}
