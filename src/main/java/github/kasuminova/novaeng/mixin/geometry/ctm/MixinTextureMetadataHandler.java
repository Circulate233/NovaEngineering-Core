package github.kasuminova.novaeng.mixin.geometry.ctm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.geometry.block.CtmMaterialProbe;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.util.TextureMetadataHandler;

import java.util.function.Function;

@Mixin(value = TextureMetadataHandler.class, remap = false)
public abstract class MixinTextureMetadataHandler {
    @WrapOperation(method = "wrap", at = @At(value = "INVOKE",
        target = "Lteam/chisel/ctm/client/model/ModelCTM;bake(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;"), require = 1)
    private IBakedModel nova$probe(final ModelCTM ctm, final IModelState state, final VertexFormat format,
                                   final Function<ResourceLocation, TextureAtlasSprite> getter, final Operation<IBakedModel> original) {
        return CtmMaterialProbe.run(ctm, () -> original.call(ctm, state, format, getter));
    }
}
