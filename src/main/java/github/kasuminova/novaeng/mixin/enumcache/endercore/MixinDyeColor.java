package github.kasuminova.novaeng.mixin.enumcache.endercore;

import com.enderio.core.common.util.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DyeColor.class, remap = false)
public abstract class MixinDyeColor {

    @Shadow
    @Final
    private static DyeColor[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DyeColor[] values() {
        return $VALUES;
    }
}
