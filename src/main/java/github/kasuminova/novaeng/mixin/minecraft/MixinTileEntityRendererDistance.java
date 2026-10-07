package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the dispatcher gate for world TESRs that bypass Actinium's normal block-entity list.
 */
@Mixin(value = TileEntityRendererDispatcher.class, remap = false)
public abstract class MixinTileEntityRendererDistance {
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"),
            cancellable = true, remap = true, require = 1)
    private void nova$skipDistantRenderer(final TileEntity tileentityIn, final float partialTicks,
                                          final int destroyStage, final CallbackInfo ci) {
        if (TileEntityRenderCulling.shouldSkip(tileentityIn)) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;getRenderer(Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;"),
        remap = true,
        require = 1
    )
    private TileEntitySpecialRenderer<TileEntity> nova$reusePreparedRenderer(TileEntityRendererDispatcher instance, TileEntity tileEntityIn) {
        return TileEntityRenderCulling.consumePreparedRenderer(instance, tileEntityIn);
    }
}
