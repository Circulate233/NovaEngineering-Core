package github.kasuminova.novaeng.mixin.geometry;

import com.sumirelabs.celeritasextra.client.ItemFrameLodState;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Celeritas Extra owns RenderItem's two call sites. Its LOD filter can retain selected faces.
 */
@Mixin(value = ItemFrameLodState.class, remap = false)
public abstract class MixinGeometryItemFrameLod {
    @Shadow
    public static boolean active;

    @Inject(method = "filterLodQuads", at = @At("HEAD"), cancellable = true)
    private static void novaeng$read(IBakedModel model, IBlockState state, EnumFacing side, long rand,
                                     CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (!active && model instanceof CompactModel compact)
            cir.setReturnValue(compact.novaeng$readQuads(state, side, rand));
    }
}
