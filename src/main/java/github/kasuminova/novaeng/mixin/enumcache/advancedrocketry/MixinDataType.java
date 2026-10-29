package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.api.DataStorage.DataType;

@Mixin(value = DataType.class, remap = false)
public abstract class MixinDataType {

    @Shadow
    @Final
    private static DataType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DataType[] values() {
        return $VALUES;
    }
}
