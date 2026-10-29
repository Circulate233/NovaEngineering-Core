package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.ItemIC2Boat.BoatType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BoatType.class, remap = false)
public abstract class MixinBoatType {

    @Shadow
    @Final
    private static BoatType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BoatType[] values() {
        return $VALUES;
    }
}
