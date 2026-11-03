package github.kasuminova.novaeng.mixin.lumenized;

import gregtech.client.utils.BloomEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BloomEffectUtil.class, remap = false)
public abstract class MixinBloomRenderLayerCapacity {
    @ModifyArg(method = "initBloomRenderLayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BufferBuilder;<init>(I)V", remap = true), index = 0, require = 1)
    private static int nova$lazyBloomFallback(final int original) {
        return Math.min(original, 1024);
    }
}
