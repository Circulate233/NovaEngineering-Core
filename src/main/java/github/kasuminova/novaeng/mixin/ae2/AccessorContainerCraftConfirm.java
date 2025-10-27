package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.networking.crafting.ICraftingJob;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.CraftingCPURecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(value = ContainerCraftConfirm.class, remap = false)
public interface AccessorContainerCraftConfirm {

    @Accessor("result")
    ICraftingJob n$getResult();

    @Accessor("cpus")
    ArrayList<CraftingCPURecord> n$getCpus();
}
