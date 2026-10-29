package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketOredictionificatorGui.OredictionificatorGuiPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OredictionificatorGuiPacket.class, remap = false)
public abstract class MixinOredictionificatorGuiPacket {

    @Shadow
    @Final
    private static OredictionificatorGuiPacket[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OredictionificatorGuiPacket[] values() {
        return $VALUES;
    }
}
