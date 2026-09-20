package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.model.async.ModelTextureGetterContext;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/** Publishes the current bake texture getter to repaired asynchronous model implementations. */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader", remap = false)
public abstract class MixinModelLoaderTextureContext {

    @WrapOperation(
        method = "setupModelRegistry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/model/IModel;bake(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;",
            remap = false),
        require = 0)
    private IBakedModel nova$withTextureGetter(final IModel model,
                                                final IModelState state,
                                                final VertexFormat format,
                                                final Function<ResourceLocation, TextureAtlasSprite> getter,
                                                final Operation<IBakedModel> original) {
        return ModelTextureGetterContext.with(getter,
            () -> original.call(model, state, format, getter));
    }
}
