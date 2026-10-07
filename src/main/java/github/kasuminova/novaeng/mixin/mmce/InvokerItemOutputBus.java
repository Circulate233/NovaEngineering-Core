package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TileItemOutputBus.class, remap = false)
public interface InvokerItemOutputBus {
    @Invoker("outputToExternal")
    void nova$invokeOutput(IItemHandler external);
}
