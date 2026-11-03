package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Actinium retains vanilla fallback builders; allocate their storage when geometry actually arrives.
 */
@Mixin(RegionRenderCacheBuilder.class)
public abstract class MixinRegionRenderCacheBuilderCapacity {
    @ModifyArg(method = "<init>()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BufferBuilder;<init>(I)V"), index = 0, require = 4)
    private int nova$initialFallbackCapacity(final int original) {
        return Math.min(original, 1024);
    }
}
