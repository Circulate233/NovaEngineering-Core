package github.kasuminova.novaeng.mixin.geometry.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.google.common.cache.LoadingCache;
import github.kasuminova.novaeng.client.geometry.block.CompactBlockModel;
import github.kasuminova.novaeng.client.geometry.block.CtmMaterialProbe;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelWrapper {
    @Shadow
    public abstract IBakedModel bakeImpl(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> getter);

    @WrapOperation(method = "bake", at = @At(value = "INVOKE",
        target = "Lcom/google/common/cache/LoadingCache;getUnchecked(Ljava/lang/Object;)Ljava/lang/Object;"), require = 0)
    private Object nova$cacheTemporaryBake(final LoadingCache<?, ?> cache, final Object key, final Operation<Object> original,
                                          @Local(argsOnly = true) final IModelState state, @Local(argsOnly = true) final VertexFormat format,
                                          @Local(argsOnly = true) final Function<ResourceLocation, TextureAtlasSprite> getter) {
        final CtmMaterialProbe probe = CtmMaterialProbe.forGetter(getter);
        if (probe == null) return original.call(cache, key);
        return probe.bake(this, key, () -> bakeImpl(state, format, getter));
    }

    @ModifyExpressionValue(method = "bakeNormal", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/model/SimpleBakedModel$Builder;makeBakedModel()Lnet/minecraft/client/renderer/block/model/IBakedModel;", remap = true), require = 1)
    private IBakedModel novaeng$compactOwnedGeometry(final IBakedModel original, @Local(argsOnly = true) final Function<?, ?> getter) {
        if (CtmMaterialProbe.isUnpublished(this, getter)) return original;
        return CompactBlockModel.fromVanillaBake((SimpleBakedModel) original);
    }
}
