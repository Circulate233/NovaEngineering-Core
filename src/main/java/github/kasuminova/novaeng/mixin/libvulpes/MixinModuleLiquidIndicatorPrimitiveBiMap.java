package github.kasuminova.novaeng.mixin.libvulpes;

import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;
import github.kasuminova.novaeng.mixin.minecraft.forge.AccessorFluidRegistryIds;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import zmaster587.libVulpes.inventory.modules.ModuleLiquidIndicator;

@Mixin(value = ModuleLiquidIndicator.class, remap = false)
public abstract class MixinModuleLiquidIndicatorPrimitiveBiMap {
    /**
     * @author NovaEngineering
     * @reason Replace a boxed linear scan with the primitive inverse lookup.
     */
    @Overwrite
    private Fluid getFluid(final int id) {
        final Fluid fluid = (Fluid) PrimitiveBiMaps.getIntKey(AccessorFluidRegistryIds.nova$getFluidIds().inverse(), id);
        return fluid == null ? FluidRegistry.WATER : fluid;
    }

    /**
     * @author NovaEngineering
     * @reason Read the fluid ID directly from primitive storage.
     */
    @Overwrite
    private int getFluidID(final Fluid fluid) {
        return PrimitiveBiMaps.getIntValue(AccessorFluidRegistryIds.nova$getFluidIds(), fluid);
    }
}
