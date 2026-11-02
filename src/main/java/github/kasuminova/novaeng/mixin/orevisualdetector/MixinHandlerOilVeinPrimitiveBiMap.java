package github.kasuminova.novaeng.mixin.orevisualdetector;

import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;
import github.kasuminova.novaeng.mixin.minecraft.forge.AccessorFluidRegistryIds;

import glodblock.com.github.handlers.HandlerOilVein;

import net.minecraftforge.fluids.Fluid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = HandlerOilVein.class, remap = false)
public abstract class MixinHandlerOilVeinPrimitiveBiMap {
    /**
     * @author NovaEngineering
     * @reason Read the registered fluid ID without the boxed public-map view.
     */
    @Overwrite
    public static int getFluidID(final Fluid fluid) {
        return PrimitiveBiMaps.getIntValue(AccessorFluidRegistryIds.nova$getFluidIds(), fluid);
    }

    /**
     * @author NovaEngineering
     * @reason Resolve a numeric fluid ID through the primitive inverse.
     */
    @Overwrite
    public static Fluid getFluid(final int id) {
        return (Fluid)
            PrimitiveBiMaps.getIntKey(
                AccessorFluidRegistryIds.nova$getFluidIds().inverse(), id);
    }
}
