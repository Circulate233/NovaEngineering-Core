package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.shapes.BeaconType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BeaconType.class, remap = false)
public abstract class MixinBeaconType {

    @Shadow
    @Final
    private static BeaconType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BeaconType[] values() {
        return $VALUES;
    }
}
