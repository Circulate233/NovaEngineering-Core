package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.item.magnet.PacketMagnetState.SlotType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SlotType.class, remap = false)
public abstract class MixinSlotType {

    @Shadow
    @Final
    private static SlotType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SlotType[] values() {
        return $VALUES;
    }
}
