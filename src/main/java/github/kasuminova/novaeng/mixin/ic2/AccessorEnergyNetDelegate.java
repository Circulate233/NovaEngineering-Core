package github.kasuminova.novaeng.mixin.ic2;

import ic2.core.block.comp.Energy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "ic2.core.block.comp.Energy$EnergyNetDelegate", remap = false)
public interface AccessorEnergyNetDelegate {
    @Accessor("this$0")
    Energy nova$getEnergy();
}
