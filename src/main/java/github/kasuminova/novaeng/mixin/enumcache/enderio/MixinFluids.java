package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.fluid.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Fluids.class, remap = false)
public abstract class MixinFluids {

    @Shadow
    @Final
    private static Fluids[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Fluids[] values() {
        return $VALUES;
    }
}
