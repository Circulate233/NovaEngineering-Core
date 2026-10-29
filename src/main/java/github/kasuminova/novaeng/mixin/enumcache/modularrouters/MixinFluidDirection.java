package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.item.module.FluidModule.FluidDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FluidDirection.class, remap = false)
public abstract class MixinFluidDirection {

    @Shadow
    @Final
    private static FluidDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FluidDirection[] values() {
        return $VALUES;
    }
}
