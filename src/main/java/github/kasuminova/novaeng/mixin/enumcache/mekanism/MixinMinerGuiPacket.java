package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketDigitalMinerGui.MinerGuiPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MinerGuiPacket.class, remap = false)
public abstract class MixinMinerGuiPacket {

    @Shadow
    @Final
    private static MinerGuiPacket[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MinerGuiPacket[] values() {
        return $VALUES;
    }
}
