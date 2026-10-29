package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.animation.TimeValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TimeValues.IdentityValue.class, remap = false)
public abstract class MixinIdentityValue {

    @Shadow
    @Final
    private static TimeValues.IdentityValue[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TimeValues.IdentityValue[] values() {
        return $VALUES;
    }
}
