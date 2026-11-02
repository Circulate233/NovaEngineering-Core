package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.ModelFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelFluid.FluidLoader.class, remap = false)
public abstract class MixinFluidLoader {

    @Shadow
    @Final
    private static ModelFluid.FluidLoader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelFluid.FluidLoader[] values() {
        return $VALUES;
    }
}
