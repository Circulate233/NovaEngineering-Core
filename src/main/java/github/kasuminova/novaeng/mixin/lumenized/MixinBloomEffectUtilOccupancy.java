package github.kasuminova.novaeng.mixin.lumenized;

import github.kasuminova.novaeng.client.bloom.BloomTerrainOccupancyImpl;
import gregtech.client.utils.BloomEffectUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Uses Lumenized's established no-FBO branch when both custom tickets and Actinium bloom terrain
 * are authoritatively empty after the original scheduling phase.
 */
@Mixin(value = BloomEffectUtil.class, remap = false)
public abstract class MixinBloomEffectUtilOccupancy {

    @Inject(method = "renderBloomBlockLayer",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getFramebuffer()Lnet/minecraft/client/shader/Framebuffer;",
            ordinal = 0,
            remap = true),
        cancellable = true,
        remap = false,
        require = 1
    )
    private static void nova$skipEmptyBloomFramebuffer(final RenderGlobal renderGlobal,
                                                       final BlockRenderLayer blockRenderLayer,
                                                       final double partialTicks,
                                                       final int pass,
                                                       final Entity entity,
                                                       final CallbackInfoReturnable<Integer> cir) {
        if (!AccessorBloomEffectUtil.nova$getBloomRenders().isEmpty()) {
            return;
        }
        if (!BloomTerrainOccupancyImpl.instance()
            .isDefinitelyEmpty(BloomEffectUtil.getBloomLayer())) {
            return;
        }

        AccessorBloomEffectUtil.nova$postDraw();
        GlStateManager.depthMask(false);
        cir.setReturnValue(renderGlobal.renderBlockLayer(
            blockRenderLayer, partialTicks, pass, entity));
    }
}
