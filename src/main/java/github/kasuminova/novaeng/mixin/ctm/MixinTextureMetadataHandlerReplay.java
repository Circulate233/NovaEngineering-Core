package github.kasuminova.novaeng.mixin.ctm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.client.ctm.CtmBakeTraceIndex;
import github.kasuminova.novaeng.client.ctm.CtmBakeTraceIndexImpl;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.util.TextureMetadataHandler;

import java.util.function.Function;

/** Replays only same-parameter duplicate simple-wrap CTM bakes within one reload generation. */
@Mixin(value = TextureMetadataHandler.class, remap = false)
public abstract class MixinTextureMetadataHandlerReplay {

    @WrapOperation(method = "wrap(Lnet/minecraftforge/client/model/IModel;Lnet/minecraft/client/renderer/block/model/IBakedModel;)Lnet/minecraft/client/renderer/block/model/IBakedModel;",
        at = @At(value = "INVOKE",
            target = "Lteam/chisel/ctm/client/model/ModelCTM;bake(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;",
            remap = false),
        remap = false, require = 1)
    private IBakedModel nova$replayDuplicateWrap(
        final ModelCTM wrapper,
        final IModelState state,
        final VertexFormat format,
        final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
        final Operation<IBakedModel> original,
        @Local(argsOnly = true) final IModel sourceModel,
        @Local(argsOnly = true) final IBakedModel existingParent
    ) {
        final CtmBakeTraceIndex index = CtmBakeTraceIndexImpl.instance();
        return index.bakeOrReplay(wrapper, sourceModel, state, format, bakedTextureGetter, existingParent,
            () -> original.call(wrapper, state, format, bakedTextureGetter));
    }
}
