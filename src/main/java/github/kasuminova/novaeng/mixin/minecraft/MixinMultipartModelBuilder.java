package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.base.Predicate;
import github.kasuminova.novaeng.client.model.MultipartModelCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(MultipartBakedModel.Builder.class)
public abstract class MixinMultipartModelBuilder {
    @Redirect(method = "makeMultipartModel", at = @At(value = "NEW",
        target = "net/minecraft/client/renderer/block/model/MultipartBakedModel"), require = 1)
    private MultipartBakedModel nova$reuseMultipartModel(final Map<Predicate<IBlockState>, IBakedModel> selectors) {
        return MultipartModelCache.create(selectors);
    }
}
