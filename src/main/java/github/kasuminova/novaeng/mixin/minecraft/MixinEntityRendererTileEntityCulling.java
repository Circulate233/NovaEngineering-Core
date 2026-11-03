package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the camera scope balanced when RenderGlobal is cancelled or a renderer throws.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererTileEntityCulling {
    @Redirect(
        method = "renderWorldPass(IFJ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"),
        require = 2
    )
    private void nova$renderEntitiesWithCamera(final RenderGlobal renderer, final Entity view,
                                               final ICamera camera, final float partialTicks) {
        TileEntityRenderCulling.begin(view, camera, partialTicks);
        try {
            renderer.renderEntities(view, camera, partialTicks);
        } finally {
            TileEntityRenderCulling.end();
        }
    }
}
