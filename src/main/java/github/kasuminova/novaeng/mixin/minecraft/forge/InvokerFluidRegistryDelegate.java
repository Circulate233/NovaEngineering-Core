package github.kasuminova.novaeng.mixin.minecraft.forge;

import net.minecraftforge.fluids.FluidRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FluidRegistry.FluidDelegate.class, remap = false)
public interface InvokerFluidRegistryDelegate {
    @Invoker("rebind")
    void nova$rebind();
}
