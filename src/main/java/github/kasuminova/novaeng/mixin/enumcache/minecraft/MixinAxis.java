package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFacing.Axis.class, remap = false)
public abstract class MixinAxis {

    @Shadow
    @Final
    private static EnumFacing.Axis[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFacing.Axis[] values() {
        return $VALUES;
    }
}
