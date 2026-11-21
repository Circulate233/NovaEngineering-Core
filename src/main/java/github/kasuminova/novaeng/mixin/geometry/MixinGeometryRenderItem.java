package github.kasuminova.novaeng.mixin.geometry;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(RenderItem.class)
public abstract class MixinGeometryRenderItem {
    @Redirect(method = "renderModel(Lnet/minecraft/client/renderer/block/model/IBakedModel;ILnet/minecraft/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/IBakedModel;getQuads(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;"), require = 2)
    private List<BakedQuad> novaeng$read(IBakedModel model, IBlockState state, EnumFacing side, long seed) {
        return model instanceof CompactModel compact ? compact.novaeng$readQuads(state, side, seed) : model.getQuads(state, side, seed);
    }
}
