package github.kasuminova.novaeng.mixin.astralsorcery;

import github.kasuminova.novaeng.client.memory.RenderBufferCapacity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "hellfirepvp.astralsorcery.client.util.BufferBatch", remap = false)
public abstract class MixinBufferBatchCapacity {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BufferBuilder;<init>(I)V", remap = true), index = 0, require = 1)
    private int nova$smallInitialBuffer(final int requested) {
        return RenderBufferCapacity.initialInts(requested);
    }
}
