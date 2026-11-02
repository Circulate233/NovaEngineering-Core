package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFacing.AxisDirection.class, remap = false)
public abstract class MixinAxisDirection {

    @Shadow
    @Final
    private static EnumFacing.AxisDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFacing.AxisDirection[] values() {
        return $VALUES;
    }
}
