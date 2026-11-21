package github.kasuminova.novaeng.mixin.geometry;

import github.kasuminova.novaeng.client.geometry.item.CompactItemRendering;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render owned item layers directly when their format and renderer support it.
 */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class MixinGeometryLitItem {
    @Inject(method = "renderLitItem", at = @At("HEAD"), cancellable = true)
    private static void novaeng$renderLayers(final RenderItem ri, final IBakedModel model, final int color,
                                             final ItemStack stack, final CallbackInfo ci) {
        if (CompactItemRendering.renderIfSupported(ri, model, color, stack)) ci.cancel();
    }
}
