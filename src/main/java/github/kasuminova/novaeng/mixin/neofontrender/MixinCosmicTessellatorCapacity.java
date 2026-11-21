package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.memory.RenderBufferCapacity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "neofontrender.core.font.cosmic.CosmicTextRenderer", remap = false)
public abstract class MixinCosmicTessellatorCapacity {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/Tessellator;<init>(I)V", remap = true), index = 0, require = 1)
    private static int nova$smallFontBuffer(final int requested) {
        return RenderBufferCapacity.initialInts(requested);
    }
}
