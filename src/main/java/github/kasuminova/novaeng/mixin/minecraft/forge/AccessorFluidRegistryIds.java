package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.BiMap;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FluidRegistry.class, remap = false)
public interface AccessorFluidRegistryIds {
    @Accessor("fluidIDs")
    static BiMap<Fluid, Integer> nova$getFluidIds() {
        throw new AssertionError();
    }
}
