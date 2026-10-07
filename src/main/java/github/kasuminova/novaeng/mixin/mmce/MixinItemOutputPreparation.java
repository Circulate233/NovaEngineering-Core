package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.AeOutputPreparation;
import github.kasuminova.novaeng.common.performance.AeOutputTarget;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileItemOutputBus.class, remap = false)
public abstract class MixinItemOutputPreparation {
    @Redirect(method = "doRestrictedTick", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/tiles/TileItemOutputBus;outputToExternal(Lnet/minecraftforge/items/IItemHandler;)V"), require = 1)
    private void nova$prepareSupportedOutput(final TileItemOutputBus bus, final IItemHandler external) {
        if (!(external instanceof AeOutputTarget)) {
            ((InvokerItemOutputBus) bus).nova$invokeOutput(external);
            return;
        }
        AeOutputPreparation.enter();
        try {
            ((InvokerItemOutputBus) bus).nova$invokeOutput(external);
        } finally {
            AeOutputPreparation.leave();
        }
    }
}
