package github.kasuminova.novaeng.mixin.geometry.actinium;

import com.dhj.actinium.api.render.terrain.BlockQuadTransformerHolder;
import com.dhj.actinium.render.terrain.compile.pipeline.VintageBlockRenderer;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.TrustedGeometryList;
import github.kasuminova.novaeng.client.geometry.item.CompactItemModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = VintageBlockRenderer.class, remap = false)
public abstract class MixinGeometryBlockRenderer {
    @Redirect(method = "renderBlock(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lcom/dhj/actinium/world/cloned/ActiniumBlockAccess;Lnet/minecraft/util/BlockRenderLayer;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/IBakedModel;getQuads(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;", remap = true), require = 2)
    private List<BakedQuad> novaeng$read(IBakedModel model, IBlockState state, EnumFacing side, long seed) {
        return model instanceof CompactModel compact && !(model instanceof CompactItemModel) && AccessorGeometryTransformers.novaeng$transformers().isEmpty() ? compact.novaeng$readQuads(state, side, seed) : model.getQuads(state, side, seed);
    }

    @Redirect(method = "renderBlock(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lcom/dhj/actinium/world/cloned/ActiniumBlockAccess;Lnet/minecraft/util/BlockRenderLayer;Z)V", at = @At(value = "INVOKE", target = "Lcom/dhj/actinium/api/render/terrain/BlockQuadTransformerHolder;transform(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lcom/dhj/actinium/world/cloned/ActiniumBlockAccess;Lnet/minecraft/util/BlockRenderLayer;Lnet/minecraft/util/EnumFacing;Ljava/util/List;)Ljava/util/List;"), require = 2)
    private List<BakedQuad> novaeng$transform(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer, EnumFacing side, List<BakedQuad> quads, @Local(name = "model") IBakedModel model) {
        if (AccessorGeometryTransformers.novaeng$transformers().isEmpty()) return quads;
        if (quads instanceof TrustedGeometryList)
            quads = model.getQuads(state, side, MathHelper.getPositionRandom(pos));
        return BlockQuadTransformerHolder.transform(state, pos, blockAccess, layer, side, quads);
    }
}
