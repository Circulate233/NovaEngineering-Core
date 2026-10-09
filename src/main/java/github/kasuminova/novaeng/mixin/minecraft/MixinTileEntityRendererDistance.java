package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the dispatcher gate for world TESRs that bypass Actinium's normal block-entity list.
 */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDistance {

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"),
        cancellable = true, require = 1)
    private void nova$skipDistantRenderer(final TileEntity tileentityIn, final float partialTicks,
                                          final int destroyStage, final CallbackInfo ci) {
        if (TileEntityRenderCulling.shouldSkip(tileentityIn)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("TAIL"), require = 1)
    private void nova$clear(final TileEntity tileentityIn, final float partialTicks,
                                          final int destroyStage, final CallbackInfo ci) {
        TileEntityRenderCulling.clearRenderer();
    }

    /**
     * @author circulation
     * @reason 复用捕获的TileEntitySpecialRenderer
     */
    @Nullable
    @Overwrite
    public <T extends TileEntity> TileEntitySpecialRenderer<T> getRenderer(@Nullable TileEntity tileEntityIn) {
        return TileEntityRenderCulling.consumePreparedRenderer((TileEntityRendererDispatcher) (Object) this, tileEntityIn);
    }

}
