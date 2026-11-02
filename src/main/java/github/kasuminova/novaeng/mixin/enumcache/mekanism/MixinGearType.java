package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.network.PacketGearStateUpdate.GearType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GearType.class, remap = false)
public abstract class MixinGearType {

    @Shadow
    @Final
    private static GearType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GearType[] values() {
        return $VALUES;
    }
}
