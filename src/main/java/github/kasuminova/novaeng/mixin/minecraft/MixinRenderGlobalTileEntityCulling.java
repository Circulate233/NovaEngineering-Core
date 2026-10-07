package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.render.TileEntityRenderCulling;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Supplies the active render pass's camera without an extra GL matrix readback.
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalTileEntityCulling {
    // A redirect at one caller would miss nested portal/offscreen renderers using another camera.
    // The method boundary also guarantees scope cleanup when a renderer throws or cancels early.
    @WrapMethod(method = "renderEntities", require = 1)
    private void nova$withTileEntityCamera(final Entity renderViewEntity, final ICamera camera, final float partialTicks,
                                           final Operation<Void> original) {
        TileEntityRenderCulling.begin(renderViewEntity, camera, partialTicks);
        try {
            original.call(renderViewEntity, camera, partialTicks);
        } finally {
            TileEntityRenderCulling.end();
        }
    }
}
