package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.implementations.CraftingCPURecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftingCPURecord.class, remap = false)
public interface AccessorCraftingCPURecord {

    @Accessor("cpu")
    ICraftingCPU n$getCpu();
}
