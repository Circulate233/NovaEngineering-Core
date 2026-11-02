package github.kasuminova.novaeng.mixin.enumcache.thermalexpansion;

import cofh.thermalexpansion.network.PacketTEBase.PacketTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketTypes.class, remap = false)
public abstract class MixinPacketTypes {

    @Shadow
    @Final
    private static PacketTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketTypes[] values() {
        return $VALUES;
    }
}
