package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplies the active render pass's camera without an extra GL matrix readback.
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalTileEntityCulling {
    @Inject(
        method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
        at = @At("HEAD"),
        require = 1
    )
    private void nova$beginTileEntityCamera(final Entity renderViewEntity, final ICamera camera,
                                            final float partialTicks, final CallbackInfo ci) {
        TileEntityRenderCulling.begin(renderViewEntity, camera, partialTicks);
    }

    @Inject(
        method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
        at = @At("RETURN"),
        require = 1
    )
    private void nova$endTileEntityCamera(final Entity renderViewEntity, final ICamera camera,
                                          final float partialTicks, final CallbackInfo ci) {
        TileEntityRenderCulling.end();
    }
}
