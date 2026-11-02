package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.animation.TimeValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TimeValues.CommonTimeValueTypeAdapterFactory.class, remap = false)
public abstract class MixinCommonTimeValueTypeAdapterFactory {

    @Shadow
    @Final
    private static TimeValues.CommonTimeValueTypeAdapterFactory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TimeValues.CommonTimeValueTypeAdapterFactory[] values() {
        return $VALUES;
    }
}
