package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.api.crops.CropSoilType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CropSoilType.class, remap = false)
public abstract class MixinCropSoilType {

    @Shadow
    @Final
    private static CropSoilType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CropSoilType[] values() {
        return $VALUES;
    }
}
