package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.common.network.PacketGeneralType;

@Mixin(value = PacketGeneralType.class, remap = false)
public abstract class MixinPacketGeneralType {

    @Shadow
    @Final
    private static PacketGeneralType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketGeneralType[] values() {
        return $VALUES;
    }
}
